package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentRevisionTest {
    @Test
    void testNotValidRevisionSwallowed() {
        DocumentRevision revision1 = new DocumentRevision("1as2", "baselineName");
        DocumentRevision revision2 = DocumentRevision.builder().name("12").build();

        assertDoesNotThrow(() -> revision1.compareTo(revision2));
    }

    @Test
    void testRevisionComparison() {
        DocumentRevision revision1 = new DocumentRevision("123", "baselineName");
        DocumentRevision revision2 = DocumentRevision.builder().name("234").build();

        assertEquals(1, revision1.compareTo(revision2));
    }

    @Test
    void testDoesNotFailOnInvalidRevision() {
        DocumentRevision revision1 = new DocumentRevision("asd", "baselineName");
        DocumentRevision revision2 = DocumentRevision.builder().name("234").build();

        assertDoesNotThrow(() -> revision1.compareTo(revision2));
    }

}
