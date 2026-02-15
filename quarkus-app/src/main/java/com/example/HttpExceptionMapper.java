package com.example;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.ValidationException;
import org.jboss.logging.Logger;

@Provider
public class HttpExceptionMapper implements ExceptionMapper<ValidationException> {
    private static final Logger LOG = Logger.getLogger(HttpExceptionMapper.class);

    @Override
    public Response toResponse(ValidationException e) {
        LOG.error("ValidationException occurred {}", e);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .build();
    }
}
