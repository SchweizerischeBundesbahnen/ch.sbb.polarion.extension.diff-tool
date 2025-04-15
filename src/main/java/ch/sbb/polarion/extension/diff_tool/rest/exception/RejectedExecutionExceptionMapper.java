package ch.sbb.polarion.extension.diff_tool.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.concurrent.RejectedExecutionException;

public class RejectedExecutionExceptionMapper implements ExceptionMapper<RejectedExecutionException> {
    private static final Logger logger = Logger.getLogger(RejectedExecutionExceptionMapper.class);

    public Response toResponse(RejectedExecutionException e) {
        logger.error("RejectedExecutionException error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.TOO_MANY_REQUESTS.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
