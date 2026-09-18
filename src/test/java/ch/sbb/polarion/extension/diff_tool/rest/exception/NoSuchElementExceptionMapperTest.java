package ch.sbb.polarion.extension.diff_tool.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoSuchElementExceptionMapperTest {

    @Test
    void testMapsNoSuchElementExceptionToNotFoundResponse() {
        NoSuchElementExceptionMapper mapper = new NoSuchElementExceptionMapper();
        NoSuchElementException exception = new NoSuchElementException("Chapter merge job is unknown: J-1");

        try (Response response = mapper.toResponse(exception)) {

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

            assertEquals("Chapter merge job is unknown: J-1", ((ErrorEntity) response.getEntity()).getMessage());
        }
    }

}
