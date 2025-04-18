package ch.sbb.polarion.extension.diff_tool.rest.exception;

import ch.sbb.polarion.extension.diff_tool.service.queue.QueueFullException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class QueueFullExceptionMapper implements ExceptionMapper<QueueFullException> {
    private static final Logger logger = Logger.getLogger(QueueFullExceptionMapper.class);

    public Response toResponse(QueueFullException e) {
        logger.error("QueueFullException error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.TOO_MANY_REQUESTS.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
