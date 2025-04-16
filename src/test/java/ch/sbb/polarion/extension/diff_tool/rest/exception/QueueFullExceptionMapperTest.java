package ch.sbb.polarion.extension.diff_tool.rest.exception;

import ch.sbb.polarion.extension.diff_tool.service.queue.QueueFullException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueFullExceptionMapperTest {

    @Test
    public void testMapsRejectedExecutionExceptionToTooManyRequestsResponse() {
        QueueFullExceptionMapper mapper = new QueueFullExceptionMapper();
        QueueFullException exception = new QueueFullException("Too many requests", new RejectedExecutionException("Queue is full"));

        try (Response response = mapper.toResponse(exception)) {

            assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

            assertEquals("Too many requests", ((ErrorEntity) response.getEntity()).getMessage());
        }
    }

}
