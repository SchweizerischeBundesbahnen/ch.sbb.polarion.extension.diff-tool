package ch.sbb.polarion.extension.diff_tool.util;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void testRemoveComments_fromString_withSingleComment() {
        String content = "Before <span id=\"polarion-comment:123\"></span> After";
        String result = CommentUtils.removeComments(content);

        assertEquals("Before  After", result);
    }

    @Test
    void testRemoveComments_fromString_withMultipleComments() {
        String content = "Start <span id=\"polarion-comment:123\"></span> Middle <span id=\"polarion-comment:456\"></span> End";
        String result = CommentUtils.removeComments(content);

        assertEquals("Start  Middle  End", result);
    }

    @Test
    void testRemoveComments_fromString_withSingleQuotes() {
        String content = "Text <span id='polarion-comment:789'>some content</span> more text";
        String result = CommentUtils.removeComments(content);

        // The regex matches the opening tag with optional closing </span>, so with content inside, it only removes opening tag
        assertEquals("Text some content</span> more text", result);
    }

    @Test
    void testRemoveComments_fromString_withNoComments() {
        String content = "Just plain text";
        String result = CommentUtils.removeComments(content);

        assertEquals("Just plain text", result);
    }

    @Test
    void testRemoveComments_fromString_withNullContent() {
        assertThrows(Exception.class, () -> CommentUtils.removeComments((String) null));
    }

    @Test
    void testRemoveComments_fromString_withEmptyContent() {
        String content = "";
        String result = CommentUtils.removeComments(content);

        assertEquals("", result);
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

}
