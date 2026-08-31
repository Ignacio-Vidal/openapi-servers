package com.example;

import io.quarkus.oidc.client.spi.TokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Attaches the OIDC client's access token to every outgoing REST Client call.
 *
 * <p>The point of this filter is what it does <em>not</em> do: it never fetches a token. The token
 * is acquired and kept fresh by a background task, so this filter only reads whatever is already
 * cached. That is what keeps token acquisition off the request path — an inflight request must
 * never pay the latency of a round trip to the OIDC provider.
 *
 * <p>The background refresh is enabled by {@code quarkus.oidc-client.refresh-interval} together
 * with {@code quarkus.oidc-client.refresh-token-time-skew} (see application.properties). The
 * interval schedules a Vert.x periodic timer that calls the token producer off-request; the skew
 * makes that timer refresh a token that is merely <em>near</em> expiry rather than waiting for it
 * to actually expire. Without the skew the timer only acts once the token is already dead, which
 * leaves exactly the latency window this filter exists to avoid.
 *
 * <p><strong>Injecting {@link TokenProvider} is load-bearing.</strong> {@code refresh-interval} is
 * only honoured for {@code AbstractTokensProducer}-based beans, which is what {@code TokenProvider}
 * delegates to. Injecting {@code OidcClient} and calling {@code getTokens()} directly bypasses the
 * shared cache and re-acquires a token on the caller's thread — the exact behaviour being avoided
 * here. See OidcClientConfig#refreshInterval() in the Quarkus source.
 *
 * <p>Registered explicitly per-client via {@code QuarkusRestClientBuilder.register(...)} rather
 * than with {@code @Provider}, so that only the downstream calls that need the token carry it.
 */
@ApplicationScoped
public class OidcClientTokenFilter implements ClientRequestFilter {

    private static final Logger LOG = Logger.getLogger(OidcClientTokenFilter.class);

    /**
     * How long to wait for the cached token. This is a safety net for the very first call, not a
     * budget for a token round trip: once the background refresh is running the token is already
     * in memory and this resolves immediately. A timeout here means the cache was empty when it
     * should not have been, which is a misconfiguration worth failing loudly on.
     */
    private static final Duration TOKEN_TIMEOUT = Duration.ofSeconds(10);

    private final TokenProvider tokenProvider;

    @Inject
    public OidcClientTokenFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        long startNanos = System.nanoTime();

        String accessToken = tokenProvider.getAccessToken()
                .await().atMost(TOKEN_TIMEOUT);

        long elapsedMicros = (System.nanoTime() - startNanos) / 1_000;

        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // Logged so the benefit is observable rather than assumed. With the background refresh
        // working this stays in the microseconds even across token expiry; a spike to hundreds of
        // milliseconds means a token was fetched on this thread.
        LOG.debugf("Attached access token to %s in %d us", requestContext.getUri(), elapsedMicros);
    }
}
