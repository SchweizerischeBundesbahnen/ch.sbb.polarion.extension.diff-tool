package ch.sbb.polarion.extension.diff_tool.util;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CommentUtilsTest {

    @Test
    void testExtractCommentIds_fromString_withSingleComment() {
        String content = "<span id=\"polarion-comment:123\"></span>";
        List<String> result = CommentUtils.extractCommentIds(content);

        assertEquals(1, result.size());
        assertEquals("123", result.get(0));
    }

    @Test
    void testExtractCommentIds_fromString_withMultipleComments() {
        String content = "Some text <span id=\"polarion-comment:123\"></span> more text <span id=\"polarion-comment:456\"></span>";
        List<String> result = CommentUtils.extractCommentIds(content);

        assertEquals(2, result.size());
        assertEquals("123", result.get(0));
        assertEquals("456", result.get(1));
    }

    @Test
    void testExtractCommentIds_fromString_withSingleQuotes() {
        String content = "<span id='polarion-comment:789'>content</span>";
        List<String> result = CommentUtils.extractCommentIds(content);

        assertEquals(1, result.size());
        assertEquals("789", result.get(0));
    }

    @Test
    void testExtractCommentIds_fromString_withNoComments() {
        String content = "Some text without comments";
        List<String> result = CommentUtils.extractCommentIds(content);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractCommentIds_fromString_withNullContent() {
        List<String> result = CommentUtils.extractCommentIds((String) null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractCommentIds_fromString_withEmptyContent() {
        String content = "";
        List<String> result = CommentUtils.extractCommentIds(content);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractCommentIds_fromElement_withDirectCommentSpan() {
        Element element = new Element("span").attr("id", "polarion-comment:123");
        List<String> result = CommentUtils.extractCommentIds(element);

        assertEquals(1, result.size());
        assertEquals("123", result.get(0));
    }

    @Test
    void testExtractCommentIds_fromElement_withNestedComments() {
        Element parent = new Element("div");
        Element child1 = new Element("span").attr("id", "polarion-comment:123");
        Element child2 = new Element("span").attr("id", "polarion-comment:456");
        parent.appendChild(child1);
        parent.appendChild(child2);

        List<String> result = CommentUtils.extractCommentIds(parent);

        assertEquals(2, result.size());
        assertEquals("123", result.get(0));
        assertEquals("456", result.get(1));
    }

    @Test
    void testExtractCommentIds_fromElement_withDeeplyNestedComments() {
        Element root = new Element("div");
        Element level1 = new Element("div");
        Element level2 = new Element("p");
        Element comment = new Element("span").attr("id", "polarion-comment:999");

        root.appendChild(level1);
        level1.appendChild(level2);
        level2.appendChild(comment);

        List<String> result = CommentUtils.extractCommentIds(root);

        assertEquals(1, result.size());
        assertEquals("999", result.get(0));
    }

    @Test
    void testExtractCommentIds_fromElement_withNoComments() {
        Element element = new Element("div");
        element.appendChild(new Element("p").text("Some text"));

        List<String> result = CommentUtils.extractCommentIds(element);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractCommentIds_fromElement_withNonMatchingSpan() {
        Element element = new Element("span").attr("id", "some-other-id");
        List<String> result = CommentUtils.extractCommentIds(element);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAppendComments_toString_withSingleComment() {
        String content = "Original content";
        List<String> commentIds = List.of("123");

        String result = CommentUtils.appendComments(content, commentIds);

        assertEquals("Original content<span id=\"polarion-comment:123\"></span>", result);
    }

    @Test
    void testAppendComments_toString_withMultipleComments() {
        String content = "Original content";
        List<String> commentIds = List.of("123", "456", "789");

        String result = CommentUtils.appendComments(content, commentIds);

        assertEquals("Original content<span id=\"polarion-comment:123\"></span><span id=\"polarion-comment:456\"></span><span id=\"polarion-comment:789\"></span>", result);
    }

    @Test
    void testAppendComments_toString_withEmptyCommentList() {
        String content = "Original content";
        List<String> commentIds = List.of();

        String result = CommentUtils.appendComments(content, commentIds);

        assertEquals("Original content", result);
    }

    @Test
    void testAppendComments_toString_withEmptyContent() {
        String content = "";
        List<String> commentIds = List.of("123");

        String result = CommentUtils.appendComments(content, commentIds);

        assertEquals("<span id=\"polarion-comment:123\"></span>", result);
    }

    @Test
    void testAppendComments_toElementList_withSingleComment() {
        List<Element> elements = new ArrayList<>();
        List<String> commentIds = List.of("123");

        CommentUtils.appendComments(elements, commentIds);

        assertEquals(1, elements.size());
        assertEquals("span", elements.get(0).tagName());
        assertEquals("polarion-comment:123", elements.get(0).attr("id"));
    }

    @Test
    void testAppendComments_toElementList_withMultipleComments() {
        List<Element> elements = new ArrayList<>();
        List<String> commentIds = List.of("123", "456");

        CommentUtils.appendComments(elements, commentIds);

        assertEquals(2, elements.size());
        assertEquals("polarion-comment:123", elements.get(0).attr("id"));
        assertEquals("polarion-comment:456", elements.get(1).attr("id"));
    }

    @Test
    void testAppendComments_toElementList_withExistingElements() {
        List<Element> elements = new ArrayList<>();
        elements.add(new Element("p").text("Existing"));
        List<String> commentIds = List.of("123");

        CommentUtils.appendComments(elements, commentIds);

        assertEquals(2, elements.size());
        assertEquals("p", elements.get(0).tagName());
        assertEquals("span", elements.get(1).tagName());
        assertEquals("polarion-comment:123", elements.get(1).attr("id"));
    }

    @Test
    void testAppendComments_toElementList_withEmptyCommentList() {
        List<Element> elements = new ArrayList<>();
        List<String> commentIds = List.of();

        CommentUtils.appendComments(elements, commentIds);

        assertTrue(elements.isEmpty());
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("removeCommentsTestCases")
    void testRemoveComments_fromString(String content, String expected) {
        String result = CommentUtils.removeComments(content);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> removeCommentsTestCases() {
        return Stream.of(
                Arguments.argumentSet("Single comment",
                        "Before <span id=\"polarion-comment:123\"></span> After",
                        "Before  After"),
                Arguments.argumentSet("Multiple comments",
                        "Start <span id=\"polarion-comment:123\"></span> Middle <span id=\"polarion-comment:456\"></span> End",
                        "Start  Middle  End"),
                Arguments.argumentSet("Single quotes",
                        "Text <span id='polarion-comment:789'>some content</span> more text",
                        "Text some content</span> more text"),
                Arguments.argumentSet("No comments",
                        "Just plain text",
                        "Just plain text"),
                Arguments.argumentSet("Empty content",
                        "",
                        "")
        );
    }

    @Test
    void testRemoveComments_fromString_withNullContent() {
        assertThrows(Exception.class, () -> CommentUtils.removeComments((String) null));
    }


    @Test
    void testRemoveComments_fromElement_withDirectCommentSpan() {
        Element parent = new Element("div");
        Element comment = new Element("span").attr("id", "polarion-comment:123");
        Element text = new Element("p").text("Text");
        parent.appendChild(comment);
        parent.appendChild(text);

        Element result = CommentUtils.removeComments(parent);

        assertSame(parent, result);
        assertEquals(1, parent.children().size());
        assertEquals("p", parent.child(0).tagName());
    }

    @Test
    void testRemoveComments_fromElement_withMultipleComments() {
        Element parent = new Element("div");
        parent.appendChild(new Element("span").attr("id", "polarion-comment:123"));
        parent.appendChild(new Element("p").text("Text"));
        parent.appendChild(new Element("span").attr("id", "polarion-comment:456"));

        CommentUtils.removeComments(parent);

        assertEquals(1, parent.children().size());
        assertEquals("p", parent.child(0).tagName());
    }

    @Test
    void testRemoveComments_fromElement_withNestedComments() {
        Element root = new Element("div");
        Element nested = new Element("div");
        nested.appendChild(new Element("span").attr("id", "polarion-comment:123"));
        nested.appendChild(new Element("p").text("Text"));
        root.appendChild(nested);

        CommentUtils.removeComments(root);

        assertEquals(1, root.children().size());
        Element nestedResult = root.child(0);
        assertEquals(1, nestedResult.children().size());
        assertEquals("p", nestedResult.child(0).tagName());
    }

    @Test
    void testRemoveComments_fromElement_withNoComments() {
        Element parent = new Element("div");
        parent.appendChild(new Element("p").text("Text 1"));
        parent.appendChild(new Element("p").text("Text 2"));

        CommentUtils.removeComments(parent);

        assertEquals(2, parent.children().size());
    }

    @Test
    void testRemoveComments_fromElement_withNonMatchingSpan() {
        Element parent = new Element("div");
        parent.appendChild(new Element("span").attr("id", "other-id"));

        CommentUtils.removeComments(parent);

        assertEquals(1, parent.children().size());
    }

    @Test
    void testRemoveComments_fromElement_withEmptyElement() {
        Element element = new Element("div");

        CommentUtils.removeComments(element);

        assertTrue(element.children().isEmpty());
    }

    @Test
    void testCopyCommentMarkers_exactPositionMatch() {
        String source = "Hello <span id=\"polarion-comment:5\"></span>World";
        String target = "Hello World";
        Map<String, String> map = Map.of("5", "50");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("Hello <span id=\"polarion-comment:50\"></span>World", result);
    }

    @Test
    void testCopyCommentMarkersByContext_multipleMarkers() {
        String source = "AAA<span id=\"polarion-comment:1\"></span>BBB<span id=\"polarion-comment:2\"></span>CCC";
        String target = "AAABBBCCC";
        Map<String, String> map = Map.of("1", "10", "2", "20");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertTrue(result.contains("<span id=\"polarion-comment:10\"></span>"));
        assertTrue(result.contains("<span id=\"polarion-comment:20\"></span>"));
        assertEquals("AAA<span id=\"polarion-comment:10\"></span>BBB<span id=\"polarion-comment:20\"></span>CCC", result);
    }

    @Test
    void testCopyCommentMarkers_fallbackToAppend() {
        String source = "<span id=\"polarion-comment:1\"></span>";
        String target = "completely different content";
        Map<String, String> map = Map.of("1", "10");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertTrue(result.startsWith("completely different content"));
        assertTrue(result.contains("<span id=\"polarion-comment:10\"></span>"));
    }

    @Test
    void testCopyCommentMarkers_unmappedIdsSkipped() {
        String source = "Hello <span id=\"polarion-comment:5\"></span>World";
        String target = "Hello World";

        String result = CommentUtils.copyCommentMarkers(source, target, Collections.emptyMap());

        assertEquals("Hello World", result);
    }

    @Test
    void testCopyCommentMarkers_emptySource() {
        String result = CommentUtils.copyCommentMarkers("", "target content", Map.of("1", "2"));

        assertEquals("target content", result);
    }

    @Test
    void testCopyCommentMarkers_onlyAfterContext() {
        // Marker at the very start of source — no "before" context, only "after"
        String source = "<span id=\"polarion-comment:1\"></span>World of text";
        String target = "World of text";
        Map<String, String> map = Map.of("1", "10");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("<span id=\"polarion-comment:10\"></span>World of text", result);
    }

    @Test
    void testCopyCommentMarkers_onlyAfterContext_noMatchInTarget() {
        // Marker at start, "after" context doesn't exist in target — marker is dropped
        String source = "<span id=\"polarion-comment:1\"></span>unique source text";
        String target = "completely different target";
        Map<String, String> map = Map.of("1", "10");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("completely different target", result);
    }

    @Test
    void testCopyCommentMarkers_onlyBeforeContext() {
        // Marker at the very end of source — no "after" context, only "before"
        String source = "Hello world<span id=\"polarion-comment:1\"></span>";
        String target = "Hello world";
        Map<String, String> map = Map.of("1", "10");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("Hello world<span id=\"polarion-comment:10\"></span>", result);
    }

    @Test
    void testCopyCommentMarkers_onlyBeforeContext_noMatchInTarget() {
        // Marker at end, "before" context doesn't exist in target — marker is dropped
        String source = "unique source text<span id=\"polarion-comment:1\"></span>";
        String target = "completely different target";
        Map<String, String> map = Map.of("1", "10");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("completely different target", result);
    }

    @Test
    void testCopyCommentMarkers_intoADuplicatedDocumentPage() {
        // a document page and its duplicate: Polarion's duplicate drops the markers and names the work items anew
        String source = "<h1 id=\"polarion_wiki macro name=module-workitem;params=id=EL-3856\"></h1><p id=\"polarion_template_1\">\n"
                + "  THIS DOCUMENT HAS NO CONTENT YET.<span id=\"polarion-comment:1\"></span>\n</p><p id=\"polarion_template_2\">\n"
                + "  YOU CAN<span id=\"polarion-comment:2\"></span> CREATE AND SAVE<span id=\"polarion-comment:3\"></span> CONTENT NOW USING THIS EDITOR."
                + "<span id=\"polarion-comment:4\"></span>\n</p><div id=\"polarion_wiki macro name=module-workitem;params=id=EL-3862\"></div>";
        String target = "<h1 id=\"polarion_wiki macro name=module-workitem;params=id=WI-373\"></h1><p id=\"polarion_template_1\">\n"
                + "  THIS DOCUMENT HAS NO CONTENT YET.\n</p><p id=\"polarion_template_2\">\n"
                + "  YOU CAN CREATE AND SAVE CONTENT NOW USING THIS EDITOR.\n</p><div id=\"polarion_wiki macro name=module-workitem;params=id=WI-375\"></div>";
        Map<String, String> map = Map.of("1", "6", "2", "7", "3", "8", "4", "9");

        String result = CommentUtils.copyCommentMarkers(source, target, map);

        assertEquals("<h1 id=\"polarion_wiki macro name=module-workitem;params=id=WI-373\"></h1><p id=\"polarion_template_1\">\n"
                + "  THIS DOCUMENT HAS NO CONTENT YET.<span id=\"polarion-comment:6\"></span>\n</p><p id=\"polarion_template_2\">\n"
                + "  YOU CAN<span id=\"polarion-comment:7\"></span> CREATE AND SAVE<span id=\"polarion-comment:8\"></span> CONTENT NOW USING THIS EDITOR."
                + "<span id=\"polarion-comment:9\"></span>\n</p><div id=\"polarion_wiki macro name=module-workitem;params=id=WI-375\"></div>", result);
    }

    @Test
    void testCopyCommentMarkers_nextToAWorkItemWhichTheTargetNamesDifferently() {
        String source = "<div id=\"polarion_wiki macro name=module-workitem;params=id=EL-1\"></div><span id=\"polarion-comment:1\"></span><p>Text after the work item</p>";
        String target = "<div id=\"polarion_wiki macro name=module-workitem;params=id=WI-9\"></div><p>Text after the work item</p>";

        String result = CommentUtils.copyCommentMarkers(source, target, Map.of("1", "10"));

        assertEquals("<div id=\"polarion_wiki macro name=module-workitem;params=id=WI-9\"></div><span id=\"polarion-comment:10\"></span><p>Text after the work item</p>", result);
    }

    @Test
    void testCopyCommentMarkers_aContextWhichOccursTwiceInTheTargetPlacesNothing() {
        String source = "<div id=\"polarion_wiki macro name=module-workitem;params=id=EL-1\"></div><span id=\"polarion-comment:1\"></span><p>Same</p>";
        String target = "<div id=\"polarion_wiki macro name=module-workitem;params=id=WI-9\"></div><p>Same</p><p>Same</p>";

        String result = CommentUtils.copyCommentMarkers(source, target, Map.of("1", "10"));

        assertEquals(target, result);
    }

    @Test
    void testCopyCommentMarkers_adjacentMarkersKeepTheirOrder() {
        String source = "AAA<span id=\"polarion-comment:1\"></span><span id=\"polarion-comment:2\"></span>BBB";
        String target = "AAABBB";

        String result = CommentUtils.copyCommentMarkers(source, target, Map.of("1", "10", "2", "20"));

        assertEquals("AAA<span id=\"polarion-comment:10\"></span><span id=\"polarion-comment:20\"></span>BBB", result);
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("markerForms")
    void testAMarkerIsFoundInEveryFormPolarionAccepts(String marker) {
        String content = "<p>before" + marker + "after</p>";

        assertEquals(List.of("5"), CommentUtils.extractCommentIds(content));
        assertEquals("<p>beforeafter</p>", CommentUtils.removeComments(content));
        assertEquals("<p>before<span id=\"polarion-comment:17\"></span>after</p>", CommentUtils.remapComments(content, Map.of("5", "17")));
    }

    private static Stream<Arguments> markerForms() {
        return Stream.of(
                Arguments.argumentSet("plain", "<span id=\"polarion-comment:5\"></span>"),
                Arguments.argumentSet("ID after another attribute", "<span class=\"x\" id=\"polarion-comment:5\"></span>"),
                Arguments.argumentSet("ID between other attributes", "<span class=\"x\" id=\"polarion-comment:5\" style=\"y\"></span>"),
                Arguments.argumentSet("upper case", "<SPAN ID=\"polarion-comment:5\"></SPAN>"),
                Arguments.argumentSet("self-closing", "<span class=\"x\" id=\"polarion-comment:5\"/>")
        );
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("notMarkers")
    void testAnythingElseIsNotTakenForAMarker(String content) {
        assertTrue(CommentUtils.extractCommentIds(content).isEmpty());
        assertEquals(content, CommentUtils.removeComments(content));
        assertEquals(content, CommentUtils.remapComments(content, Map.of("5", "17")));
    }

    private static Stream<Arguments> notMarkers() {
        return Stream.of(
                Arguments.argumentSet("an ordinary span", "<span class=\"x\">text</span>"),
                Arguments.argumentSet("another attribute ending in id", "<span data-id=\"polarion-comment:5\"></span>"),
                Arguments.argumentSet("another element", "<div id=\"polarion-comment:5\"></div>"),
                Arguments.argumentSet("a span-like element", "<spanner id=\"polarion-comment:5\"></spanner>")
        );
    }

    @Test
    void testASpanNextToAMarkerIsLeftAsItIs() {
        String content = "<span class=\"x\">a</span><span class=\"y\"></span><span id=\"polarion-comment:5\"></span><span>b</span>";

        assertEquals(List.of("5"), CommentUtils.extractCommentIds(content));
        assertEquals("<span class=\"x\">a</span><span class=\"y\"></span><span>b</span>", CommentUtils.removeComments(content));
        assertEquals("<span class=\"x\">a</span><span class=\"y\"></span><span id=\"polarion-comment:17\"></span><span>b</span>",
                CommentUtils.remapComments(content, Map.of("5", "17")));
    }

    @Test
    void testTheFirstMarkerIdOfASpanIsTaken() {
        String content = "<span id=\"polarion-comment:5\" id=\"polarion-comment:6\"></span>";

        assertEquals(List.of("5"), CommentUtils.extractCommentIds(content));
        assertEquals("<span id=\"polarion-comment:17\"></span>", CommentUtils.remapComments(content, Map.of("5", "17", "6", "18")));
    }

    @Test
    void testAMarkerIsCopiedNextToASpanWhichIsNoMarker() {
        String source = "<span class=\"x\">Hello</span> <span id=\"polarion-comment:5\"></span>World";
        String target = "<span class=\"x\">Hello</span> World";

        String result = CommentUtils.copyCommentMarkers(source, target, Map.of("5", "50"));

        assertEquals("<span class=\"x\">Hello</span> <span id=\"polarion-comment:50\"></span>World", result);
    }

    @Test
    @Timeout(5)
    void testASpanWhichIsNeverClosedTakesLinearTime() {
        String content = "<span" + " id=\"polarion-comment:5\"".repeat(50_000);
        Map<String, String> idMapping = Map.of("5", "17");

        assertTrue(CommentUtils.extractCommentIds(content).isEmpty());
        assertEquals(content, CommentUtils.removeComments(content));
        assertEquals(content, CommentUtils.remapComments(content, idMapping));
        assertEquals("target", CommentUtils.copyCommentMarkers(content, "target", idMapping));
    }

    @Test
    void testRemapComments_newIdOfOneCommentIsTheOldIdOfAnother() {
        String content = "<span id=\"polarion-comment:5\"></span>a<span id=\"polarion-comment:6\"></span>";

        // one pass over the text: a marker which was already re-numbered must not be re-numbered again
        assertEquals("<span id=\"polarion-comment:6\"></span>a<span id=\"polarion-comment:7\"></span>",
                CommentUtils.remapComments(content, Map.of("5", "6", "6", "7")));
    }

}
