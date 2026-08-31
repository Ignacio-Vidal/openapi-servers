package com.example;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.oidc.client.spi.TokenProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the access token is refreshed in the background rather than on the request path.
 *
 * <p>The realm mints tokens with a 5-second lifespan (see {@code quarkus-realm.json}), so a test
 * that runs for ~15 seconds crosses several expiries. That is the whole point: under the lazy
 * default, every crossing would stall an incoming request while a new token was fetched from
 * Keycloak. With {@code refresh-interval} configured, the timer replaces the token between
 * requests and callers never see it.
 */
@QuarkusTest
class BackgroundTokenRefreshTest extends QuarkusRestClientTestBase {

    /** Comfortably longer than the 5s token lifespan, so several expiries are crossed. */
    private static final Duration RUN_DURATION = Duration.ofSeconds(15);

    /**
     * A token fetch from Keycloak is a network round trip and costs milliseconds. Reading an
     * already-cached token is a memory read. This threshold sits far below any real round trip
     * but well above a cache read, so it separates the two without being flaky.
     */
    private static final long CACHE_READ_CEILING_MICROS = 50_000;

    @Inject
    TokenProvider tokenProvider;

    @Test
    @DisplayName("tokens rotate in the background while request latency stays flat")
    void tokenRefreshesInBackgroundWithoutBlockingRequests() throws Exception {
        org.eclipse.microprofile.config.Config cfg =
                org.eclipse.microprofile.config.ConfigProvider.getConfig();
        for (String k : new String[]{"quarkus.oidc-client.refresh-interval",
                "quarkus.oidc-client.refresh-token-time-skew",
                "quarkus.oidc-client.grant.type",
                "quarkus.oidc-client.auth-server-url"}) {
            System.out.println("### cfg " + k + " = " + cfg.getOptionalValue(k, String.class));
        }

        Set<String> observedTokens = new HashSet<>();
        List<Long> attachMicros = new ArrayList<>();

        long deadline = System.nanoTime() + RUN_DURATION.toNanos();
        while (System.nanoTime() < deadline) {
            // Read the token the same way the filter does. If acquisition were happening on the
            // request path, this is where the latency would show up.
            long start = System.nanoTime();
            String token = tokenProvider.getAccessToken().await().atMost(Duration.ofSeconds(10));
            attachMicros.add((System.nanoTime() - start) / 1_000);

            observedTokens.add(token);
            Thread.sleep(500);
        }

        // The tokens must actually have rotated -- otherwise the test proves nothing about
        // expiry, it just proves a single token stayed valid for the whole run.
        assertThat(observedTokens)
                .as("token should have been replaced several times across a 15s run of 5s tokens")
                .hasSizeGreaterThan(1);

        // And no caller should ever have waited for that to happen.
        System.out.println("### attachMicros=" + attachMicros);
        System.out.println("### distinctTokens=" + observedTokens.size());
        assertThat(attachMicros)
                .as("every token read should come from cache, never a round trip to Keycloak")
                .allSatisfy(micros -> assertThat(micros).isLessThan(CACHE_READ_CEILING_MICROS));
    }

    @Test
    @DisplayName("unauthenticated caller reaches a secured downstream endpoint via the filter")
    void relayAttachesTokenToDownstreamCall() {
        // No credentials on the way in; the filter supplies them on the way out.
        given()
                .when().get("/downstream/relay")
                .then()
                .statusCode(200)
                .body("downstream.authenticated", org.hamcrest.Matchers.is(true));
    }

    @Test
    @DisplayName("the downstream endpoint really does reject unauthenticated calls")
    void securedEndpointRejectsAnonymousAccess() {
        // Guards against the relay test passing because the downstream is not actually secured.
        given()
                .when().get("/downstream/secured")
                .then()
                .statusCode(401);
    }
}
