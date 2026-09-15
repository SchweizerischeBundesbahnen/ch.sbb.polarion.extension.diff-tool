package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentMergePair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.types.Text;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentsContentHandlerTest {

    DocumentsContentHandler handler = new DocumentsContentHandler();

    private static String anchor(String tag, String workItemId) {
        return "<%s id=\"polarion_wiki macro name=module-workitem;params=id=%s\"></%s>".formatted(tag, workItemId, tag);
    }

    private static String text(String content) {
        return "<p id=\"polarion_1\">%s</p>".formatted(content);
    }

    @Test
    void testParseAnchors() {
        Map<String, DocumentContentAnchor> contentAnchors = handler.parse("""
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-2"></h3>
            <p>Paragraph above</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-3"></div>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-4"></div>
            <p>Paragraph below the last</p>
        """);
        assertEquals(4, contentAnchors.size());
        assertTrue(contentAnchors.containsKey("AA-1"));
        assertTrue(contentAnchors.containsKey("AA-2"));
        assertTrue(contentAnchors.containsKey("AA-3"));
        assertTrue(contentAnchors.containsKey("AA-4"));
        assertEquals("<p>Paragraph above</p>", contentAnchors.get("AA-3").getContentAbove());
        assertEquals("<p>Paragraph below the last</p>", contentAnchors.get("AA-4").getContentBelow());
    }

    @Test
    void testParseNoAnchors() {
        assertTrue(handler.parse("<p>Some paragraph</p>").isEmpty());
    }

    @Test
    void testMergeDocuments() {
        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getHomePageContent()).thenReturn(Text.html("""
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-2"></h3>
            <p>Paragraph above</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-3"></div>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-4"></div>
        """));

        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getHomePageContent()).thenReturn(Text.html("""
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-5"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-6"></h3>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-7"></div>
            <p>Paragraph below</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-8"></div>
        """));
        ArgumentCaptor<Text> valueCapture = ArgumentCaptor.forClass(Text.class);
        doNothing().when(rightDocument).setHomePageContent(valueCapture.capture());

        DocumentsContentMergeContext context = new DocumentsContentMergeContext(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class), MergeDirection.LEFT_TO_RIGHT, false);
        List<DocumentsContentMergePair> pairsToMerge = new ArrayList<>();
        pairsToMerge.add(DocumentsContentMergePair.builder().leftWorkItemId("AA-3").rightWorkItemId("AA-7").contentPosition(DocumentContentAnchor.ContentPosition.ABOVE).build());
        handler.merge(leftDocument, rightDocument, context, pairsToMerge);

        String expectedResult = """
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-5"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-6"></h3>
            <p>Paragraph above</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-7"></div>
            <p>Paragraph below</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-8"></div>""";

        assertEquals(expectedResult, valueCapture.getValue().getContent());
    }

    @Test
    void testGetContentAboveAnchor() {
        Document document = generateTestDocument();
        List<Element> elements = handler.getContent(document, "AA-3", DocumentContentAnchor.ContentPosition.ABOVE);
        assertNotNull(elements);
        assertEquals(1, elements.size());
        Element element = elements.get(0);
        assertEquals("p", element.tagName());
        assertEquals("Paragraph above", element.text());
    }

    @Test
    void testGetContentBelowAnchor() {
        Document document = generateTestDocument();
        List<Element> elements = handler.getContent(document, "AA-3", DocumentContentAnchor.ContentPosition.BELOW);
        assertNotNull(elements);
        assertTrue(elements.isEmpty());
    }

    @Test
    void testGetContentAtTheEndOfDoc() {
        Document document = generateTestDocument();
        List<Element> elements = handler.getContent(document, "AA-4", DocumentContentAnchor.ContentPosition.BELOW);
        assertEquals("<p>Paragraph below the last</p>", elements.get(0).toString());

        elements = handler.getContent(document, "AA-4", DocumentContentAnchor.ContentPosition.ABOVE);
        assertTrue(elements.isEmpty());

        elements = handler.getContent(document, "BB-0", DocumentContentAnchor.ContentPosition.BELOW);
        assertTrue(elements.isEmpty());

        elements = handler.getContent(Jsoup.parse(""), "AA-1", DocumentContentAnchor.ContentPosition.BELOW);
        assertTrue(elements.isEmpty());
    }

    @Test
    void testInsertContentAboveAnchor() {
        Document document = generateTestDocument();

        Element element = new Element("p");
        element.text("merged");
        boolean contentModified = handler.insertContent(document, "AA-3", DocumentContentAnchor.ContentPosition.ABOVE, Collections.singletonList(element), false);
        assertTrue(contentModified);

        assertEquals(6, document.body().children().size());

        element = document.body().children().get(0);
        assertEquals("h2", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-1", element.id());

        element = document.body().children().get(1);
        assertEquals("h3", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-2", element.id());

        element = document.body().children().get(2);
        assertEquals("p", element.tagName());
        assertEquals("merged", element.text());

        element = document.body().children().get(3);
        assertEquals("div", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-3", element.id());
    }

    @Test
    void testInsertContentBelowAnchor() {
        Document document = generateTestDocument();

        Element element = new Element("p");
        element.text("merged");
        boolean contentModified = handler.insertContent(document, "AA-3", DocumentContentAnchor.ContentPosition.BELOW, Collections.singletonList(element), true);
        assertTrue(contentModified);

        assertEquals(7, document.body().children().size());

        element = document.body().children().get(0);
        assertEquals("h2", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-1", element.id());

        element = document.body().children().get(1);
        assertEquals("h3", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-2", element.id());

        element = document.body().children().get(2);
        assertEquals("p", element.tagName());
        assertEquals("Paragraph above", element.text());

        element = document.body().children().get(3);
        assertEquals("div", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-3", element.id());

        element = document.body().children().get(4);
        assertEquals("p", element.tagName());
        assertEquals("merged", element.text());
    }

    @Test
    void testRemoveContent() {
        Document document = generateTestDocument();
        boolean removed = handler.removeContent(document, document.body().children().get(0), document.body().children().get(3), new ArrayList<>());
        assertTrue(removed);

        assertEquals(4, document.body().children().size());

        Element element = document.body().children().get(0);
        assertEquals("h2", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-1", element.id());

        element = document.body().children().get(1);
        assertEquals("div", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-3", element.id());
    }

    @Test
    void testRemoveComments() {
        // Note: <span/> and <div/> are NOT self-closing in HTML5. jsoup 1.21.x
        // (Polarion 2606) parses them strictly per spec — the trailing "/" is
        // ignored and everything following nests inside. Use explicit closing
        // tags so the test fixture matches the intended tree shape.
        Document document = Jsoup.parse("""
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-2"></h3>
            <p>Paragraph above</p>
            <span id="polarion-comment:1"></span>
            <span id="some-span"></span>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-3"></div>
            <span id="polarion-comment:42"></span>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-4"></div>
            <p>Paragraph below the last</p>
        """);

        List<Element> elements = handler.getContent(document, "AA-3", DocumentContentAnchor.ContentPosition.ABOVE);
        assertNotNull(elements);
        assertEquals(2, elements.size());

        elements = handler.getContent(document, "AA-3", DocumentContentAnchor.ContentPosition.BELOW);
        assertTrue(elements.isEmpty());
    }

    @Test
    void testRemoveContentAllChildren() {
        Document document = generateTestDocument();
        boolean removed = handler.removeContent(document, null, null, new ArrayList<>());
        assertTrue(removed);

        assertTrue(document.body().children().isEmpty());
    }

    @Test
    void testInsertContentInTheMiddleOfDocument() {
        Document document = generateTestDocument();
        List<Element> elementsToInsert = new ArrayList<>();
        Element element = new Element("p");
        element.attr("id", "test1");
        elementsToInsert.add(element);
        element = new Element("div");
        element.attr("id", "test2");
        elementsToInsert.add(element);
        boolean inserted = handler.insertContent(document, document.body().children().get(1), elementsToInsert);
        assertTrue(inserted);

        assertEquals(8, document.body().children().size());

        element = document.body().children().get(0);
        assertEquals("h2", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-1", element.id());

        element = document.body().children().get(1);
        assertEquals("h3", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-2", element.id());

        element = document.body().children().get(2);
        assertEquals("p", element.tagName());
        assertEquals("test1", element.id());

        element = document.body().children().get(3);
        assertEquals("div", element.tagName());
        assertEquals("test2", element.id());
    }

    @Test
    void testInsertContentInTheBeginningOfDocument() {
        Document document = generateTestDocument();
        List<Element> elementsToInsert = new ArrayList<>();
        Element element = new Element("p");
        element.attr("id", "test1");
        elementsToInsert.add(element);
        element = new Element("div");
        element.attr("id", "test2");
        elementsToInsert.add(element);
        boolean inserted = handler.insertContent(document, null, elementsToInsert);
        assertTrue(inserted);

        assertEquals(8, document.body().children().size());

        element = document.body().children().get(0);
        assertEquals("p", element.tagName());
        assertEquals("test1", element.id());

        element = document.body().children().get(1);
        assertEquals("div", element.tagName());
        assertEquals("test2", element.id());

        element = document.body().children().get(2);
        assertEquals("h2", element.tagName());
        assertEquals("polarion_wiki macro name=module-workitem;params=id=AA-1", element.id());
    }

    @Test
    void testInvalidAnchor() {
        Element element = new Element("div");
        element.attr("id", "polarion_wiki macro name=module-workitem");
        DocumentContentAnchor anchor = handler.anchor(element);
        assertNull(anchor);
    }

    @Test
    void testValidAnchor() {
        Element element = new Element("div");
        element.attr("id", "polarion_wiki macro name=module-workitem;params=id=AA-1");
        DocumentContentAnchor anchor = handler.anchor(element);
        assertNotNull(anchor);
        assertEquals("AA-1", anchor.getId());
    }

    @Test
    void testIsChapter() {
        Element element = new Element("h1");
        assertTrue(handler.isChapter(element));

        element = new Element("h2");
        assertTrue(handler.isChapter(element));

        element = new Element("h3");
        assertTrue(handler.isChapter(element));

        element = new Element("h4");
        assertTrue(handler.isChapter(element));

        element = new Element("h5");
        assertTrue(handler.isChapter(element));

        element = new Element("h6");
        assertTrue(handler.isChapter(element));

        element = new Element("h7");
        assertFalse(handler.isChapter(element));

        element = new Element("p");
        assertFalse(handler.isChapter(element));
    }

    @Test
    void testExtractWorkItemId() {
        Element element = new Element("div");
        element.attr("id", "polarion_wiki macro name=module-workitem;params=id=AA-1");
        String id = handler.extractWorkItemId(element);
        assertEquals("AA-1", id);
    }

    private Document generateTestDocument() {
        return Jsoup.parse("""
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
            <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-2"></h3>
            <p>Paragraph above</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-3"></div>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-4"></div>
            <p>Paragraph below the last</p>
        """);
    }

    @Test
    void testCopyFreeContentOfMergedWorkItems() {
        String sourceContent = """
            <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
            <p>Text below the chapter</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-2"></div>
            <p>Text below the last item</p>
        """;

        IModule targetModule = mock(IModule.class);
        String targetContent = """
            <h2 id="polarion_wiki macro name=module-workitem;params=id=BB-1"></h2>
            <div id="polarion_wiki macro name=module-workitem;params=id=BB-2|layout=0"></div>
        """;
        when(targetModule.getHomePageContent()).thenReturn(Text.html(targetContent));

        boolean modified = handler.copyFreeContent(sourceContent, targetModule, List.of("AA-1", "AA-2"), Map.of("AA-1", "BB-1", "AA-2", "BB-2"), Map.of());

        assertTrue(modified);
        ArgumentCaptor<Text> contentCaptor = ArgumentCaptor.forClass(Text.class);
        verify(targetModule).setHomePageContent(contentCaptor.capture());
        String newContent = contentCaptor.getValue().getContent();
        assertTrue(newContent.contains("<p>Text below the chapter</p>"));
        assertTrue(newContent.contains("<p>Text below the last item</p>"));
        assertTrue(newContent.indexOf("Text below the chapter") < newContent.indexOf("id=BB-2"));
        // Everything the merge did not touch stays exactly as the editor wrote it - the page is not re-serialized
        assertTrue(newContent.contains("""
            <div id="polarion_wiki macro name=module-workitem;params=id=BB-2|layout=0"></div>"""));
        assertEquals(targetContent, newContent
                .replace("<p>Text below the chapter</p>", "")
                .replace("<p>Text below the last item</p>", ""));
    }

    @Test
    void testContentIsPlacedAroundTheAnchorItBelongsTo() {
        String anchor = "<div id=\"polarion_wiki macro name=module-workitem;params=id=BB-1\"></div>";

        assertEquals("<p>above</p>" + anchor,
                handler.insertAtAnchor(anchor, "BB-1", "<p>above</p>", DocumentContentAnchor.ContentPosition.ABOVE));
        assertEquals(anchor + "<p>below</p>",
                handler.insertAtAnchor(anchor, "BB-1", "<p>below</p>", DocumentContentAnchor.ContentPosition.BELOW));
    }

    @Test
    void testContentOfAWorkItemWhichIsNotOnThePageIsNotPlacedAnywhere() {
        String content = "<div id=\"polarion_wiki macro name=module-workitem;params=id=BB-1\"></div>";

        assertEquals(content, handler.insertAtAnchor(content, "BB-2", "<p>orphan</p>", DocumentContentAnchor.ContentPosition.BELOW));
        assertEquals(content, handler.insertAtAnchor(content, "BB-1", null, DocumentContentAnchor.ContentPosition.BELOW));
    }

    @Test
    void testAnchorOfAnotherWorkItemWithASimilarIdIsNotTakenForIt() {
        String content = "<div id=\"polarion_wiki macro name=module-workitem;params=id=BB-11\"></div>";

        assertEquals(content, handler.insertAtAnchor(content, "BB-1", "<p>content</p>", DocumentContentAnchor.ContentPosition.BELOW));
    }


    @Test
    void testTheRestOfThePageIsLeftExactlyAsPolarionWroteIt() {
        // The page is written by Polarion's own editor and read back by its own parser. A merge inserts the text
        // it copies and touches nothing else - it does not re-serialize, re-indent or re-escape the page.
        String targetContent = "<h1 id=\"polarion_wiki macro name=module-workitem;params=id=BB-1\"></h1>"
                + "<p id=\"polarion_1\">Text with a\u00a0non-breaking space and <b>markup</b></p>"
                + "<div id=\"polarion_wiki macro name=module-workitem;params=id=BB-2|layout=2\">BB-2</div>";
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(targetContent));

        handler.copyFreeContent("""
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-2"></div>
            <p>Copied text</p>
        """, targetModule, List.of("AA-2"), Map.of("AA-2", "BB-2"), Map.of());

        ArgumentCaptor<Text> contentCaptor = ArgumentCaptor.forClass(Text.class);
        verify(targetModule).setHomePageContent(contentCaptor.capture());
        String newContent = contentCaptor.getValue().getContent();
        assertEquals(targetContent, newContent.replace("<p>Copied text</p>", ""));
        // the anchor Polarion wrote with the workitem ID as its text is left as Polarion wrote it
        assertTrue(newContent.contains("params=id=BB-2|layout=2\">BB-2</div>"));
    }


    @Test
    void testACommentOfTheCopiedTextIsPointedAtTheCopiedComment() {
        String sourceContent = "<h2 id=\"polarion_wiki macro name=module-workitem;params=id=AA-1\"></h2>"
                + "<p id=\"polarion_7\">A commented paragraph<span id=\"polarion-comment:5\"></span> of the chapter</p>"
                + "<div id=\"polarion_wiki macro name=module-workitem;params=id=AA-2\"></div>";
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(
                "<h2 id=\"polarion_wiki macro name=module-workitem;params=id=BB-1\"></h2>"
                        + "<div id=\"polarion_wiki macro name=module-workitem;params=id=BB-2\"></div>"));

        handler.copyFreeContent(sourceContent, targetModule, List.of("AA-1", "AA-2"),
                Map.of("AA-1", "BB-1", "AA-2", "BB-2"), Map.of("5", "17"));

        ArgumentCaptor<Text> contentCaptor = ArgumentCaptor.forClass(Text.class);
        verify(targetModule).setHomePageContent(contentCaptor.capture());
        String newContent = contentCaptor.getValue().getContent();
        assertTrue(newContent.contains("<span id=\"polarion-comment:17\"></span>"));
        assertFalse(newContent.contains("polarion-comment:5"));
    }

    @Test
    void testTheCommentsWrittenOnTheTextAreTheOnesTheMergeHasToCopy() {
        String sourceContent = "<h2 id=\"polarion_wiki macro name=module-workitem;params=id=AA-1\"></h2>"
                + "<p>Commented<span id=\"polarion-comment:5\"></span></p>"
                + "<div id=\"polarion_wiki macro name=module-workitem;params=id=AA-2\"></div>"
                + "<p>Also commented<span id=\"polarion-comment:9\"></span></p>";

        assertEquals(Set.of("5", "9"), handler.freeContentCommentIds(sourceContent, List.of("AA-1", "AA-2")));
        // both paragraphs belong to AA-2: the text before a work item is the text above it, see parse()
        assertEquals(Set.of("5", "9"), handler.freeContentCommentIds(sourceContent, List.of("AA-2")));
        // a comment on text which is not being copied is none of the merge's business
        assertEquals(Set.of(), handler.freeContentCommentIds(sourceContent, List.of("AA-1")));
    }

    @Test
    void testAMarkerOfACommentWhichWasNotCopiedIsDropped() {
        // it would otherwise anchor to a comment the target document does not have
        assertEquals("<p>text</p>", handler.contentToInsert("<p>text<span id=\"polarion-comment:9\"></span></p>", Map.of("5", "17")));
    }


    @Test
    void testMergedWorkItemsArePlacedUnderTheChapterAboveWhatItAlreadyHeld() {
        // as Polarion leaves the page: the merged items scattered among the text and the items of the chapter
        String documentContent = anchor("h2", "BB-1") + anchor("h3", "BB-10") + text("existing")
                + anchor("div", "BB-9") + anchor("div", "BB-11") + text("more existing") + anchor("div", "BB-12");
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(documentContent));

        assertTrue(handler.moveAnchorsBelow(targetModule, List.of("BB-10", "BB-11", "BB-12"), "BB-1"));

        ArgumentCaptor<Text> contentCaptor = ArgumentCaptor.forClass(Text.class);
        verify(targetModule).setHomePageContent(contentCaptor.capture());
        String newContent = contentCaptor.getValue().getContent();
        assertEquals(anchor("h2", "BB-1") + anchor("h3", "BB-10") + anchor("div", "BB-11") + anchor("div", "BB-12")
                + text("existing") + anchor("div", "BB-9") + text("more existing"), newContent);
    }

    @Test
    void testMergedWorkItemsWhichAreAlreadyInPlaceAreNotMoved() {
        String documentContent = anchor("h2", "BB-1") + anchor("h3", "BB-10") + anchor("div", "BB-11")
                + text("existing") + anchor("div", "BB-9");
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(documentContent));

        assertFalse(handler.moveAnchorsBelow(targetModule, List.of("BB-10", "BB-11"), "BB-1"));
        verify(targetModule, never()).setHomePageContent(any());
    }

    @Test
    void testWorkItemsWhichAreNotOnThePageArePassedOver() {
        String documentContent = anchor("h2", "BB-1") + text("existing") + anchor("div", "BB-10");
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(documentContent));

        assertTrue(handler.moveAnchorsBelow(targetModule, List.of("BB-10", "BB-404"), "BB-1"));

        ArgumentCaptor<Text> contentCaptor = ArgumentCaptor.forClass(Text.class);
        verify(targetModule).setHomePageContent(contentCaptor.capture());
        assertEquals(anchor("h2", "BB-1") + anchor("div", "BB-10") + text("existing"), contentCaptor.getValue().getContent());
    }

    @Test
    void testNothingIsMovedWhenNoneOfTheWorkItemsIsOnThePage() {
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(anchor("h2", "BB-1")));

        assertFalse(handler.moveAnchorsBelow(targetModule, List.of("BB-404"), "BB-1"));
        verify(targetModule, never()).setHomePageContent(any());
    }

    @Test
    void testTheMergedWorkItemsStayOnThePageWhenTheChapterHasNoAnchor() {
        // the anchor of the chapter is written in a shape which is not recognized, so the block has nowhere to go:
        // putting the page back without it would take the just merged work items out of the document
        String documentContent = "<h2 id=\"polarion_wiki macro name=module-workitem;params=id=BB-1\"/>" + anchor("div", "BB-10") + text("existing");
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html(documentContent));

        assertFalse(handler.moveAnchorsBelow(targetModule, List.of("BB-10"), "BB-1"));
        verify(targetModule, never()).setHomePageContent(any());
    }

    @Test
    void testCopyFreeContentDoesNothingWhenThereIsNoFreeContent() {
        IModule targetModule = mock(IModule.class);
        when(targetModule.getHomePageContent()).thenReturn(Text.html("""
            <div id="polarion_wiki macro name=module-workitem;params=id=BB-1"></div>
        """));

        boolean modified = handler.copyFreeContent("""
            <div id="polarion_wiki macro name=module-workitem;params=id=AA-1"></div>
        """, targetModule, List.of("AA-1"), Map.of("AA-1", "BB-1"), Map.of());

        assertFalse(modified);
        verify(targetModule, never()).setHomePageContent(any());
    }
}
