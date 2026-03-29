package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates every response entity against its Jakarta Bean Validation constraints
 * before the response is sent to the client.
 *
 * <p>A constraint violation here is a server-side bug (the implementation returned
 * data that violates the API contract), so a {@code 500 Internal Server Error} is
 * returned together with a structured list of the violations.
 */
@ApplicationScoped
public class ResponseValidationFilter {

    private static final Logger LOG = Logger.getLogger(ResponseValidationFilter.class);

    private final Validator validator;

    @Inject
    public ResponseValidationFilter(Validator validator) {
        this.validator = validator;
    }

    @ServerResponseFilter
    public void filter(ContainerResponseContext responseContext) {

        Object entity = responseContext.getEntity();
        if (entity == null) {
            return;
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(entity);

        if (violations.isEmpty()) {
            return;
        }

        List<Map<String, String>> violationList = violations.stream()
                .map(cv -> Map.of(
                        "field",        cv.getPropertyPath().toString(),
                        "message",      cv.getMessage(),
                        "invalidValue", cv.getInvalidValue() == null
                                        ? "null"
                                        : cv.getInvalidValue().toString()
                ))
                .toList();

        LOG.errorf("Response validation failed with %d violation(s): %s",
                violations.size(), violationList);

        responseContext.setStatus(500);
        responseContext.setEntity(
                Map.of(
                        "status",     500,
                        "error",      "Response Validation Failed",
                        "violations", violationList
                ),
                new Annotation[0],
                MediaType.APPLICATION_JSON_TYPE
        );
    }
}
