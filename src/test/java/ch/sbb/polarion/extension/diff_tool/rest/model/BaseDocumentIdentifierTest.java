package ch.sbb.polarion.extension.diff_tool.rest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseDocumentIdentifierTest {

    @Test
    void pointsToSameDocumentAsReturnsTrueForIdenticalIdentifiers() {
        BaseDocumentIdentifier a = new BaseDocumentIdentifier("p", "s", "doc");
        BaseDocumentIdentifier b = new BaseDocumentIdentifier("p", "s", "doc");
        assertTrue(a.pointsToSameDocumentAs(b));
    }

    @Test
    void pointsToSameDocumentAsReturnsFalseWhenProjectDiffers() {
        BaseDocumentIdentifier a = new BaseDocumentIdentifier("pA", "s", "doc");
        BaseDocumentIdentifier b = new BaseDocumentIdentifier("pB", "s", "doc");
        assertFalse(a.pointsToSameDocumentAs(b));
    }

    @Test
    void pointsToSameDocumentAsReturnsFalseWhenSpaceDiffers() {
        BaseDocumentIdentifier a = new BaseDocumentIdentifier("p", "sA", "doc");
        BaseDocumentIdentifier b = new BaseDocumentIdentifier("p", "sB", "doc");
        assertFalse(a.pointsToSameDocumentAs(b));
    }

    @Test
    void pointsToSameDocumentAsReturnsFalseWhenNameDiffers() {
        BaseDocumentIdentifier a = new BaseDocumentIdentifier("p", "s", "left");
        BaseDocumentIdentifier b = new BaseDocumentIdentifier("p", "s", "right");
        assertFalse(a.pointsToSameDocumentAs(b));
    }

    @Test
    void pointsToSameDocumentAsReturnsFalseForNull() {
        BaseDocumentIdentifier a = new BaseDocumentIdentifier("p", "s", "doc");
        assertFalse(a.pointsToSameDocumentAs(null));
    }
}
