package com.example;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Provider
public class HttpExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    private static final Logger LOG = Logger.getLogger(HttpExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<Map<String, String>> violations = e.getConstraintViolations().stream()
                .map(cv -> Map.of(
                        "field", fieldName(cv),
                        "message", cv.getMessage(),
                        "invalidValue", cv.getInvalidValue() == null ? "null" : cv.getInvalidValue().toString()
                ))
                .toList();

        LOG.errorf("Validation failed with %d violation(s): %s", violations.size(), violations);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 400,
                        "error", "Validation Failed",
                        "violations", violations
                ))
                .build();
    }

    private String fieldName(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        // Strip method/param prefix (e.g. "createPet.petRequest.name" -> "petRequest.name")
        String[] parts = path.split("\\.");
        return parts.length > 2 ? String.join(".", java.util.Arrays.copyOfRange(parts, 2, parts.length)) : path;
    }
}
