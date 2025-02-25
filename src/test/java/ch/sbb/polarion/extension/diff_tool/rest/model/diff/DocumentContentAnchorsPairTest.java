package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentContentAnchorsPairTest {

    @Test
    void testGetLeftContentForNullAnchor() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(null, DocumentContentAnchor.builder().contentAbove("above").contentBelow("below").build());

        assertEquals("", documentContentAnchorsPair.getLeftContent(DocumentContentAnchor.ContentSide.ABOVE));
        assertEquals("", documentContentAnchorsPair.getLeftContent(DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testGetRightContentForNullAnchor() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(DocumentContentAnchor.builder().contentAbove("above").contentBelow("below").build(), null);

        assertEquals("", documentContentAnchorsPair.getRightContent(DocumentContentAnchor.ContentSide.ABOVE));
        assertEquals("", documentContentAnchorsPair.getRightContent(DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testGetLeftContentAbove() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(
                DocumentContentAnchor.builder().contentAbove("left_above").build(),
                DocumentContentAnchor.builder().contentAbove("right_above").build());

        assertEquals("left_above", documentContentAnchorsPair.getLeftContent(DocumentContentAnchor.ContentSide.ABOVE));
    }

    @Test
    void testGetRightContentAbove() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(
                DocumentContentAnchor.builder().contentAbove("left_above").contentBelow("left_below").build(),
                DocumentContentAnchor.builder().contentAbove("right_above").contentBelow("right_below").build());

        assertEquals("right_above", documentContentAnchorsPair.getRightContent(DocumentContentAnchor.ContentSide.ABOVE));
    }

    @Test
    void testGetLeftContentBelow() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(
                DocumentContentAnchor.builder().contentAbove("left_above").contentBelow("left_below").build(),
                DocumentContentAnchor.builder().contentAbove("right_above").contentBelow("right_below").build());

        assertEquals("left_below", documentContentAnchorsPair.getLeftContent(DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testGetRightContentBelow() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(
                DocumentContentAnchor.builder().contentAbove("left_above").contentBelow("left_below").build(),
                DocumentContentAnchor.builder().contentAbove("right_above").contentBelow("right_below").build());

        assertEquals("right_below", documentContentAnchorsPair.getRightContent(DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testSetLeftDiffForNullAnchor() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(null, new DocumentContentAnchor());

        assertDoesNotThrow(() -> documentContentAnchorsPair.setLeftDiff("diff", DocumentContentAnchor.ContentSide.ABOVE));
        assertDoesNotThrow(() -> documentContentAnchorsPair.setLeftDiff("diff", DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testSetRightDiffForNullAnchor() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(new DocumentContentAnchor(), null);

        assertDoesNotThrow(() -> documentContentAnchorsPair.setRightDiff("diff", DocumentContentAnchor.ContentSide.ABOVE));
        assertDoesNotThrow(() -> documentContentAnchorsPair.setRightDiff("diff", DocumentContentAnchor.ContentSide.BELOW));
    }

    @Test
    void testSetLeftDiffAbove() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(new DocumentContentAnchor(), new DocumentContentAnchor());
        documentContentAnchorsPair.setLeftDiff("left_diff_above", DocumentContentAnchor.ContentSide.ABOVE);
        assertEquals("left_diff_above", documentContentAnchorsPair.getLeftAnchor().getDiffAbove());
    }

    @Test
    void testSetRightDiffAbove() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(new DocumentContentAnchor(), new DocumentContentAnchor());
        documentContentAnchorsPair.setRightDiff("right_diff_above", DocumentContentAnchor.ContentSide.ABOVE);

        assertEquals("right_diff_above", documentContentAnchorsPair.getRightAnchor().getDiffAbove());
    }

    @Test
    void testSetLeftDiffBelow() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(new DocumentContentAnchor(), new DocumentContentAnchor());
        documentContentAnchorsPair.setLeftDiff("left_diff_below", DocumentContentAnchor.ContentSide.BELOW);

        assertEquals("left_diff_below", documentContentAnchorsPair.getLeftAnchor().getDiffBelow());
    }

    @Test
    void testSetRightDiffBelow() {
        DocumentContentAnchorsPair documentContentAnchorsPair = new DocumentContentAnchorsPair(new DocumentContentAnchor(), new DocumentContentAnchor());
        documentContentAnchorsPair.setRightDiff("right_diff_below", DocumentContentAnchor.ContentSide.BELOW);

        assertEquals("right_diff_below", documentContentAnchorsPair.getRightAnchor().getDiffBelow());
    }
}
