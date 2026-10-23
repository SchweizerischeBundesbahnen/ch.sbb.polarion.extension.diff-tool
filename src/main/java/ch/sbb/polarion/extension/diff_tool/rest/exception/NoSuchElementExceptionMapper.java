package ch.sbb.polarion.extension.diff_tool.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.NoSuchElementException;

/**
 * Answers a call which asks for something this server doesn't have with 404, the way a chapter merge job which
 * has expired, was never started here, or belongs to another user is asked for.
 */
public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {

    private static final Logger logger = Logger.getLogger(NoSuchElementExceptionMapper.class);

    @Override
    public Response toResponse(NoSuchElementException e) {
        logger.error("Unknown element: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
