package com.example;

import io.quarkus.oidc.client.spi.TokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
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
 *
 * <p><strong>Do not force this bean to be created eagerly at startup.</strong> It is tempting to
 * add an {@code @Startup} bean that resolves a token up front so the first request does not pay
 * for it. Doing so silently disables the background refresh entirely. The periodic task is
 * registered from {@code AbstractTokensProducer#init()}, which only schedules a timer when the
 * resolved client is an {@code OidcClientImpl}; if the producer is created before OIDC discovery
 * has completed, the client is still a {@code DeferredOidcClient}, the {@code instanceof} guard in
 * {@code OidcClientsImpl#registerTokenRefresh} does not match, and no timer is ever scheduled.
 * There is no warning when this happens -- refresh just quietly reverts to the request path. This
 * was observed and measured here: with an eager warmup bean, refreshes ran on the caller's thread
 * and token reads spiked to ~27ms on every expiry; without it they run on a Vert.x event loop and
 * reads stay in the hundreds of microseconds.
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

    /**
     * Resolved lazily, per call, and deliberately so.
     *
     * <p>Holding a {@code TokenProvider} directly would force the underlying token producer to be
     * created as soon as this filter is constructed -- which happens at application start, because
     * DownstreamResource injects this filter from its own {@code @PostConstruct}. Creating the
     * producer that early is exactly the failure described above: OIDC discovery has not finished,
     * the client is still a {@code DeferredOidcClient}, and the background refresh timer is never
     * scheduled. Going through {@link Instance} defers that to the first actual request, by which
     * point discovery has completed and the timer is scheduled correctly.
     */
    private final Instance<TokenProvider> tokenProvider;

    @Inject
    public OidcClientTokenFilter(Instance<TokenProvider> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Time in microseconds that the most recent call spent obtaining the token.
     *
     * <p>Exposed so tests (and the /downstream/relay endpoint) can assert on what the request path
     * actually paid, rather than on a measurement taken from a test thread. A test that calls the
     * token provider directly in a tight loop measures itself: the periodic timer and the caller
     * share one token cache, so whichever notices the token is stale first performs the refresh,
     * and a fast-polling test will frequently beat the timer to it.
     */
    private volatile long lastAttachMicros = -1;

    public long lastAttachMicros() {
        return lastAttachMicros;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        long startNanos = System.nanoTime();

        String accessToken = tokenProvider.get().getAccessToken()
                .await().atMost(TOKEN_TIMEOUT);

        long elapsedMicros = (System.nanoTime() - startNanos) / 1_000;
        lastAttachMicros = elapsedMicros;

        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        // Logged so the benefit is observable rather than assumed. With the background refresh
        // working this stays in the microseconds even across token expiry; a spike to hundreds of
        // milliseconds means a token was fetched on this thread.
        LOG.debugf("Attached access token to %s in %d us", requestContext.getUri(), elapsedMicros);
    }
}
