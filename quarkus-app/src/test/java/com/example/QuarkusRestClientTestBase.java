package com.example;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Shared setup for tests that drive the generated OpenAPI clients.
 *
 * <p>The generated interfaces are {@code @RegisterRestClient} ones, so they must be built with
 * {@link QuarkusRestClientBuilder}. That builder requires a live ArC (CDI) context, which is why
 * subclasses are {@code @QuarkusTest} classes rather than plain JUnit tests pointed at an
 * externally started server. {@code @QuarkusTest} starts the application itself and injects its
 * port via {@link TestHTTPResource}.
 *
 * <p>This module deliberately uses the Quarkus REST Client rather than RESTEasy Classic: the two
 * cannot coexist (Quarkus fails the build with "Mixing Quarkus REST and RESTEasy Classic client
 * parts is not supported"), and only the Quarkus stack can send the generated multipart
 * {@code @FormParam File} parameters.
 */
public abstract class QuarkusRestClientTestBase {

    /**
     * Overrides the server the tests drive, so the suite can be pointed at an already-running
     * application instead of the one {@code @QuarkusTest} starts.
     *
     * <p>This exists to use the tests as a traffic generator against a server you launched
     * yourself — typically in dev mode under a debugger, where you want to step through the
     * request. Set it with {@code -Dtest.base-uri=http://localhost:8080}, or use the
     * {@code testAgainstRunningServer} Gradle task which passes it for you.
     *
     * <p>Unset (the default), the tests use the port {@code @QuarkusTest} assigned, which keeps an
     * ordinary {@code test} run self-contained.
     *
     * <p>Every assertion in these tests is made on an HTTP response, so the suite is equally valid
     * in either mode — pointing it elsewhere changes which server is exercised, not what is checked.
     */
    public static final String BASE_URI_PROPERTY = "test.base-uri";

    @TestHTTPResource
    URI injectedBaseUri;

    /** The server under test: the override when one is set, otherwise this run's own server. */
    protected URI baseUri;

    @BeforeEach
    void resolveBaseUri() {
        String override = System.getProperty(BASE_URI_PROPERTY);
        baseUri = (override == null || override.isBlank())
                ? injectedBaseUri
                : URI.create(override.trim());

        RestAssured.baseURI = baseUri.getScheme() + "://" + baseUri.getHost();
        RestAssured.port = baseUri.getPort();
    }

    /** Builds a generated client interface bound to the server under test. */
    protected <T> T client(Class<T> apiInterface) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(apiInterface);
    }

    /**
     * Asserts that a call fails with the given HTTP status.
     *
     * <p>The generated client is built with {@code microprofileRegisterExceptionMapper=false} and
     * {@code microprofileGlobalExceptionMapper=false}, so the generated {@code ApiExceptionMapper}
     * is never registered and error responses surface as the REST Client's own
     * {@link WebApplicationException} subclass ({@code ClientWebApplicationException}) instead of
     * the generated checked {@code ApiException}. Asserting on the status rather than the concrete
     * type keeps these tests precise and independent of that choice.
     */
    protected static void assertFailsWithStatus(int expectedStatus, Executable call) {
        WebApplicationException thrown = assertThrows(WebApplicationException.class, call);
        assertEquals(expectedStatus, thrown.getResponse().getStatus(),
                "unexpected HTTP status");
    }
}
