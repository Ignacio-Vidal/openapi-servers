package com.example;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Normalises Jackson deserialization failures into the same error shape
 * produced by {@link HttpExceptionMapper}:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Malformed Request",
 *   "violations": [{ "field": "...", "message": "...", "invalidValue": "..." }]
 * }
 * </pre>
 *
 * Quarkus registers its own {@code BuiltinMismatchedInputExceptionMapper} at
 * priority 5100 for {@code MismatchedInputException}. Using
 * {@code @Priority(1)} on the same mapped type ensures this mapper takes
 * precedence (lower number = higher priority in JAX-RS).
 *
 * Covered cases:
 * <ul>
 *   <li>{@link InvalidTypeIdException}   – unknown discriminator value (e.g. petType: "FishRequest")</li>
 *   <li>{@link MismatchedInputException} – missing {@code @JsonProperty(required=true)} field</li>
 * </ul>
 */
@Provider
@Priority(1)
public class JsonMappingExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    private static final Logger LOG = Logger.getLogger(JsonMappingExceptionMapper.class);

    @Override
    public Response toResponse(MismatchedInputException e) {
        String field = lastFieldName(e);
        String message;
        String invalidValue = "null";

        if (e instanceof InvalidTypeIdException ite) {
            String typeId = ite.getTypeId() != null ? ite.getTypeId() : "null";
            field = field.isEmpty() ? "petType" : field;
            message = "Unknown discriminator value '" + typeId + "'";
            invalidValue = typeId;
        } else {
            message = field.isEmpty()
                    ? e.getOriginalMessage()
                    : "Missing required field '" + field + "'";
        }

        LOG.errorf("JSON deserialization failed [field=%s]: %s", field, e.getOriginalMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 400,
                        "error", "Malformed Request",
                        "violations", List.of(Map.of(
                                "field", field,
                                "message", message,
                                "invalidValue", invalidValue
                        ))
                ))
                .build();
    }

    /** Returns the last field name from the Jackson path, or an empty string when the path is absent. */
    private String lastFieldName(JsonMappingException e) {
        List<JsonMappingException.Reference> path = e.getPath();
        if (path == null || path.isEmpty()) {
            return "";
        }
        String name = path.get(path.size() - 1).getFieldName();
        return name != null ? name : "";
    }
}
