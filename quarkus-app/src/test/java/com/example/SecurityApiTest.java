package com.example;

import com.example.openapi.quarkus.client.api.SecurityTestApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class SecurityApiTest {

    static SecurityTestApi api;
    static SecurityTestApi authenticatedApi;

    /** Reads the Keycloak base URL from -Dkeycloak.url or falls back to the value used in generated-requests.http */
    private static final String KEYCLOAK_URL =
            System.getProperty("keycloak.url", "http://localhost:9090");

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

        ResteasyClient httpClient = new ResteasyClientBuilderImpl().build();
        String json = httpClient
                .target(KEYCLOAK_URL + "/realms/quarkus/protocol/openid-connect/token")
                .request()
                .post(Entity.form(form), String.class);

        try {
            return new ObjectMapper().readTree(json).get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain JWT from Keycloak at " + KEYCLOAK_URL, e);
        }
    }

    @BeforeAll
    static void setup() {
        int port = Integer.parseInt(System.getProperty("quarkus.http.port", "8080"));
        URI baseUri = URI.create("http://localhost:" + port);

        // Unauthenticated client — no Authorization header
        api = new ResteasyClientBuilderImpl().build()
                .target(baseUri)
                .proxy(SecurityTestApi.class);

        // Authenticated client — every request carries a Bearer token
        String token = obtainToken();
        authenticatedApi = new ResteasyClientBuilderImpl()
                .register(new BearerTokenFilter(token))
                .build()
                .target(baseUri)
                .proxy(SecurityTestApi.class);
    }

    // ── Without token: secured endpoints must return 401 ─────────────────────

    @Test
    void testAndAllQualify_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testAndAllQualify());
    }

    @Test
    void testApiKey_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testApiKey());
    }

    @Test
    void testHttpBasic_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testHttpBasic());
    }

    @Test
    void testHttpBearer_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testHttpBearer());
    }

    @Test
    void testMtls_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testMtls());
    }

    @Test
    void testOauth2EmptyScopes_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testOauth2EmptyScopes());
    }

    @Test
    void testOpenIdEmptyScopes_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testOpenIdEmptyScopes());
    }

    @Test
    void testOrOneQualifies_withoutToken_shouldReturn401() {
        assertThrows(NotAuthorizedException.class, () -> api.testOrOneQualifies());
    }

    // ── Without token: public endpoints must return 204 ───────────────────────

    @Test
    void testAndNotAllQualify_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> api.testAndNotAllQualify());
    }

    @Test
    void testNoSecurity_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> api.testNoSecurity());
    }

    @Test
    void testOauth2WithScopes_withoutToken_shouldReturn204() {
        assertDoesNotThrow(() -> api.testOauth2WithScopes());
    }

    // ── With valid token: secured endpoints must return 204 ───────────────────

    @Test
    void testAndAllQualify_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testAndAllQualify());
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
    void testOauth2EmptyScopes_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOauth2EmptyScopes());
    }

    @Test
    void testOpenIdEmptyScopes_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOpenIdEmptyScopes());
    }

    @Test
    void testOrOneQualifies_withToken_shouldReturn204() {
        assertDoesNotThrow(() -> authenticatedApi.testOrOneQualifies());
    }
}
