package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentsPairTest {

    @Test
    void testNoArgsConstructor() {
        DocumentsPair pair = new DocumentsPair();
        assertNull(pair.getLeftDocument());
        assertNull(pair.getRightDocument());
    }

    @Test
    void testAllArgsConstructor() {
        Document leftDoc = Document.builder().id("leftId").build();
        Document rightDoc = Document.builder().id("rightId").build();

        DocumentsPair pair = new DocumentsPair(leftDoc, rightDoc);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    @Test
    void testBuilder() {
        Document leftDoc = Document.builder().id("leftId").build();
        Document rightDoc = Document.builder().id("rightId").build();

        DocumentsPair pair = DocumentsPair.builder()
                .leftDocument(leftDoc)
                .rightDocument(rightDoc)
                .build();

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    @Test
    void testOfWithBothDocuments() {
        Document leftDoc = Document.builder().id("leftId").build();
        Document rightDoc = Document.builder().id("rightId").build();

        DocumentsPair pair = DocumentsPair.of(leftDoc, rightDoc);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }

    @Test
    void testOfWithNullRightDocument() {
        Document leftDoc = Document.builder().id("leftId").build();

        DocumentsPair pair = DocumentsPair.of(leftDoc, null);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertNull(pair.getRightDocument());
    }

    @Test
    void testSetters() {
        DocumentsPair pair = new DocumentsPair();

        Document leftDoc = Document.builder().id("newLeftId").build();
        Document rightDoc = Document.builder().id("newRightId").build();

        pair.setLeftDocument(leftDoc);
        pair.setRightDocument(rightDoc);

        assertEquals(leftDoc, pair.getLeftDocument());
        assertEquals(rightDoc, pair.getRightDocument());
    }
}
