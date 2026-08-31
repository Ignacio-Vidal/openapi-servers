package com.example;

import io.quarkus.oidc.client.spi.TokenProvider;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Forces the OIDC token producer to be created at startup instead of on the first request.
 *
 * <p>This exists because of a gap in how the periodic refresh is wired. The task that refreshes
 * the token in the background is registered from the {@code @PostConstruct} of Quarkus'
 * {@code TokensProducer}, which is a plain {@code @Singleton} with no {@code @Startup} of its own.
 * The bean the application actually injects ({@code TokenProvider}) is {@code @RequestScoped}, so
 * without this class nothing creates that singleton until traffic arrives — meaning the very first
 * request both pays the full token acquisition cost and is what starts the refresh timer.
 *
 * <p>Resolving the token here moves that cost to startup. {@code earlyTokensAcquisition} defaults
 * to {@code true}, so the token is fetched during construction and the periodic timer is armed
 * before the application serves anything.
 */
@Startup
@Singleton
public class OidcClientWarmup {

    private static final Logger LOG = Logger.getLogger(OidcClientWarmup.class);

    private final Instance<TokenProvider> tokenProvider;

    @Inject
    public OidcClientWarmup(Instance<TokenProvider> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostConstruct
    void warmup() {
        long startNanos = System.nanoTime();
        try {
            tokenProvider.get().getAccessToken().await().atMost(Duration.ofSeconds(30));
            LOG.infof("OIDC client warmed up in %d ms; periodic refresh is now armed",
                    (System.nanoTime() - startNanos) / 1_000_000);
        } catch (RuntimeException e) {
            // Deliberately not fatal: the application should still start if the provider is
            // briefly unavailable. The token producer recovers on the first request, at the cost
            // of that request paying for the acquisition.
            LOG.warnf(e, "OIDC client warmup failed; the first request will acquire the token instead");
        }
    }
}
