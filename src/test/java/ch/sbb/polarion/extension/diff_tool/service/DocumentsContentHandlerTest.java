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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentsContentHandlerTest {

    DocumentsContentHandler handler = new DocumentsContentHandler();

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

        DocumentsContentMergeContext context = new DocumentsContentMergeContext(mock(DocumentIdentifier.class), mock(DocumentIdentifier.class), MergeDirection.LEFT_TO_RIGHT);
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
    }

    @Test
    void testInsertContentAboveAnchor() {
        Document document = generateTestDocument();

        Element element = new Element("p");
        element.text("merged");
        boolean contentModified = handler.insertContent(document, "AA-3", DocumentContentAnchor.ContentPosition.ABOVE, Collections.singletonList(element));
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
        boolean contentModified = handler.insertContent(document, "AA-3", DocumentContentAnchor.ContentPosition.BELOW, Collections.singletonList(element));
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
        boolean removed = handler.removeContent(document, document.body().children().get(0), document.body().children().get(3));
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
    void testRemoveContentAllChildren() {
        Document document = generateTestDocument();
        boolean removed = handler.removeContent(document, null, null);
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
}
