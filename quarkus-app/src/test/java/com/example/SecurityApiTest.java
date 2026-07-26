package com.example;

import com.example.openapi.quarkus.client.api.PermitAllTestApi;
import com.example.openapi.quarkus.client.api.SecurityTestApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
class SecurityApiTest extends QuarkusRestClientTestBase {

    SecurityTestApi api;
    SecurityTestApi authenticatedApi;
    PermitAllTestApi permitAllApi;
    PermitAllTestApi authenticatedPermitAllApi;

    /**
     * The Keycloak token endpoint.
     *
     * <p>Under {@code @QuarkusTest} the Keycloak Dev Service starts on a random port (the
     * {@code %dev.}-scoped {@code quarkus.keycloak.devservices.port=9090} does not apply to the
     * test profile), so the realm URL is read from the {@code quarkus.oidc.auth-server-url} that
     * Dev Services injects. {@code -Dkeycloak.url} still overrides it for an external Keycloak.
     */
    private static String tokenEndpoint() {
        String override = System.getProperty("keycloak.url");
        if (override != null) {
            return override + "/realms/quarkus/protocol/openid-connect/token";
        }
        return ConfigProvider.getConfig()
                .getValue("quarkus.oidc.auth-server-url", String.class)
                + "/protocol/openid-connect/token";
    }

    /** JAX-RS filter that attaches a static Bearer token to every request. */
    record BearerTokenFilter(String token) implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext ctx) {
            ctx.getHeaders().add("Authorization", "Bearer " + token);
        }
    }

    /**
     * Obtains an access token from Keycloak using the resource owner password flow,
     * mirroring the first request in generated-requests.http.
     */
    static String obtainToken() {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "backend-service");
        form.add("client_secret", "secret");
        form.add("username", "alice");
        form.add("password", "alice");

        String endpoint = tokenEndpoint();
        try (jakarta.ws.rs.client.Client httpClient = jakarta.ws.rs.client.ClientBuilder.newClient()) {
            String json = httpClient
                    .target(endpoint)
                    .request()
                    .post(Entity.form(form), String.class);

            return new ObjectMapper().readTree(json).get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain JWT from Keycloak at " + endpoint, e);
        }
    }

    /** Cached so the Keycloak token is fetched once per class rather than per test. */
    private static String cachedToken;

    @BeforeEach
    void setupClients() {
        // Unauthenticated clients — no Authorization header
        api = client(SecurityTestApi.class);
        permitAllApi = client(PermitAllTestApi.class);

        // Authenticated clients — every request carries a Bearer token
        if (cachedToken == null) {
            cachedToken = obtainToken();
        }
        authenticatedApi = authenticatedClient(SecurityTestApi.class, cachedToken);
        authenticatedPermitAllApi = authenticatedClient(PermitAllTestApi.class, cachedToken);
    }

    /** Builds a generated client that attaches a Bearer token to every request. */
    private <T> T authenticatedClient(Class<T> apiInterface, String token) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .register(new BearerTokenFilter(token))
                .build(apiInterface);
    }

    // ── Without token: secured endpoints must return 401 ─────────────────────

    @Test
    void testAndAllQualify_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testAndAllQualify());
    }

    @Test
    void testApiKey_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testApiKey());
    }

    @Test
    void testHttpBasic_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testHttpBasic());
    }

    @Test
    void testHttpBearer_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testHttpBearer());
    }

    @Test
    void testMtls_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testMtls());
    }

    @Test
    void testOauth2EmptyScopes_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testOauth2EmptyScopes());
    }

    @Test
    void testOpenIdEmptyScopes_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testOpenIdEmptyScopes());
    }

    @Test
    void testOrOneQualifies_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testOrOneQualifies());
    }

    @Test
    void testOauth2WithScopes_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testOauth2WithScopes());
    }

    @Test
    void testAndNotAllQualify_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401, () -> api.testAndNotAllQualify());
    }

    @Test
    void testNoSecurity_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> api.testNoSecurity());
    }

    // ── With valid token: secured endpoints must return 204 ───────────────────

    @Test
    void testAndAllQualify_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testAndAllQualify());
    }

    @Test
    void testAndNotAllQualify_withToken_shouldReturn403() {
        assertFailsWithStatus(403, () -> authenticatedApi.testAndNotAllQualify());
    }

    @Test
    void testApiKey_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testApiKey());
    }

    @Test
    void testHttpBasic_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testHttpBasic());
    }

    @Test
    void testHttpBearer_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testHttpBearer());
    }

    @Test
    void testMtls_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testMtls());
    }

    @Test
    void testNoSecurity_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testNoSecurity());
    }

    @Test
    void testOauth2EmptyScopes_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOauth2EmptyScopes());
    }

    @Test
    void testOauth2WithScopes_withToken_shouldReturn403() {
        assertFailsWithStatus(403, () -> authenticatedApi.testOauth2WithScopes());
    }

    @Test
    void testOpenIdEmptyScopes_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOpenIdEmptyScopes());
    }

    @Test
    void testOrOneQualifies_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOrOneQualifies());
    }

    // ── @PermitAll endpoints (PR 23782) ───────────────────────────────────────
    // @PermitAll means anonymous access succeeds AND authenticated access still
    // succeeds — Quarkus treats it as "no role required."

    // Rule C: op-level security:[] overrides non-empty global httpBearer

    @Test
    void testPermitAllOpEmptyOverridesGlobal_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> permitAllApi.testPermitAllOpEmptyOverridesGlobal());
    }

    @Test
    void testPermitAllOpEmptyOverridesGlobal_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedPermitAllApi.testPermitAllOpEmptyOverridesGlobal());
    }

    // Rule F: OR list with anonymous alternative ({})

    @Test
    void testPermitAllOrWithAnonymous_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> permitAllApi.testPermitAllOrWithAnonymous());
    }

    @Test
    void testPermitAllOrWithAnonymous_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedPermitAllApi.testPermitAllOrWithAnonymous());
    }

    // Sanity: no op-level security inherits non-empty global → @RolesAllowed({"**"})

    @Test
    void testPermitAllInheritsNonEmptyGlobal_withoutToken_shouldReturn401() {
        assertFailsWithStatus(401,
                () -> permitAllApi.testPermitAllInheritsNonEmptyGlobal());
    }

    @Test
    void testPermitAllInheritsNonEmptyGlobal_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedPermitAllApi.testPermitAllInheritsNonEmptyGlobal());
    }
}
