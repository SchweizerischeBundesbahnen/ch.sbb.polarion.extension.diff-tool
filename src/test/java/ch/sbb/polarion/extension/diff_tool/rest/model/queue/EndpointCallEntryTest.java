package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndpointCallEntryTest {

    @Test
    void testConstructorWithParametersAndVerifyFieldValues() {
        long queued = 100L;
        long executing = 50L;

        EndpointCallEntry entry = new EndpointCallEntry(queued, executing);

        assertEquals(queued, entry.getQueued());
        assertEquals(executing, entry.getExecuting());
        assertNotNull(entry.getTimestamp());
        assertNotNull(entry.getRawTimestamp());
    }

}
