package com.example;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.security.Authenticated;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;

/**
 * Demonstrates the pattern this example exists for: an endpoint that accepts <em>unauthenticated</em>
 * incoming traffic and then makes an <em>authenticated</em> downstream call, attaching a JWT that is
 * kept fresh in the background by {@link OidcClientTokenFilter}.
 *
 * <p>The "downstream" service here loops back to this same application (to {@code /downstream/secured},
 * which does require a token), so the whole flow can be exercised with a single process. In a real
 * deployment that call would go to another service, but the token handling is identical.
 *
 * <p>{@code /downstream/relay} is the interesting one: it is {@code @PermitAll}, so callers reach it
 * with no credentials, exactly like the service being modelled. It reports how long attaching the
 * token took, which is the number that must stay flat even as tokens expire.
 */
@Path("/downstream")
@ApplicationScoped
public class DownstreamResource {

    private static final Logger LOG = Logger.getLogger(DownstreamResource.class);

    /** Minimal client for the loopback call; the generated clients are exercised elsewhere. */
    @Path("/downstream")
    public interface SecuredDownstreamApi {
        @GET
        @Path("/secured")
        @Produces(MediaType.APPLICATION_JSON)
        Map<String, Object> secured();
    }

    private final OidcClientTokenFilter tokenFilter;
    private final int httpPort;

    private SecuredDownstreamApi downstreamClient;

    @Inject
    public DownstreamResource(OidcClientTokenFilter tokenFilter,
                              @ConfigProperty(name = "quarkus.http.port") int httpPort) {
        this.tokenFilter = tokenFilter;
        this.httpPort = httpPort;
    }

    @PostConstruct
    void buildClient() {
        // The token filter is registered per-client rather than globally, so only the calls that
        // need a token carry one.
        downstreamClient = QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("http://localhost:" + httpPort))
                .register(tokenFilter)
                .build(SecuredDownstreamApi.class);
    }

    /**
     * Unauthenticated entry point that relays to a secured downstream endpoint.
     *
     * <p>{@code tokenAttachMicros} is the cost of obtaining the token for this request. It stays in
     * the microseconds while the background refresh is doing its job; it would jump to a full
     * provider round trip if the token were being fetched here instead.
     */
    @GET
    @Path("/relay")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> relay() {
        long startNanos = System.nanoTime();
        Map<String, Object> downstream = downstreamClient.secured();
        long elapsedMicros = (System.nanoTime() - startNanos) / 1_000;

        LOG.infof("Relayed to downstream in %d us", elapsedMicros);

        return Map.of(
                // Whole downstream hop, including the loopback HTTP call itself.
                "downstreamCallMicros", elapsedMicros,
                // Just the token attach -- the number that must stay flat across token expiry.
                "tokenAttachMicros", tokenFilter.lastAttachMicros(),
                "downstream", downstream);
    }

    /** Stands in for the downstream service: rejects anything without a valid JWT. */
    @GET
    @Path("/secured")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> secured(@Context SecurityContext securityContext) {
        return Map.of(
                "authenticated", true,
                "principal", securityContext.getUserPrincipal().getName());
    }
}
