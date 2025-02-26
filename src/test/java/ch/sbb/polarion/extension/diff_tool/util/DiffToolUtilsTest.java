package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.StringsDiff;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffToolUtilsTest {
    @Test
    void testWrapInSurrogateTag() {
        String text = "Some text";
        String expectedHtml = "<outer_surrogate_tag>Some text</outer_surrogate_tag>";

        String html = DiffToolUtils.wrapInSurrogateTag(text);
        assertEquals(expectedHtml, html);
    }

    @Test
    void testWrapNullInSurrogateTag() {
        String html = DiffToolUtils.wrapInSurrogateTag(null);
        assertEquals("<outer_surrogate_tag></outer_surrogate_tag>", html);
    }

    @Test
    void testUnwrapSurrogateTag() {
        assertEquals("Some info", DiffToolUtils.unwrapSurrogateTag("<outer_surrogate_tag>Some info</outer_surrogate_tag>"));
    }

    @Test
    void testApplyStyle() {
        String diffResult = "<div class=\"diff-html-removed\">Removed Text</div>";
        String expectedStyledResult = "<div style=\"background-color: #FDC6C6; text-decoration: line-through;\">Removed Text</div>";
        String styledResult = DiffToolUtils.applyStyle(diffResult);
        assertEquals(expectedStyledResult, styledResult);
    }

    @Test
    void testGetStyleByClass() {
        String removedStyle = DiffToolUtils.getStyleByClass("diff-html-removed");
        String addedStyle = DiffToolUtils.getStyleByClass("diff-html-added");
        String modifiedStyle = DiffToolUtils.getStyleByClass("diff-html-modified");
        String changedStyle = DiffToolUtils.getStyleByClass("diff-html-changed");
        String invalidStyle = DiffToolUtils.getStyleByClass("invalid-class");

        assertEquals("background-color: #FDC6C6; text-decoration: line-through;", removedStyle);
        assertEquals("background-color: #CCFFCC;", addedStyle);
        assertEquals("background-color: lightyellow;", modifiedStyle);
        assertEquals("border: 1px dashed magenta; padding: 2px; margin: 2px;", changedStyle);
        assertEquals("", invalidStyle);
    }

    @Test
    void testIsEnumContainingTypeWithEnumType() {
        IEnumType enumTypeMock = mock(IEnumType.class);
        boolean result = DiffToolUtils.isEnumContainingType(enumTypeMock);
        assertTrue(result);
    }

    @Test
    void testIsEnumContainingTypeWithListContainingEnumType() {
        IListType listTypeMock = mock(IListType.class);
        IEnumType enumTypeMock = mock(IEnumType.class);
        when(listTypeMock.getItemType()).thenReturn(enumTypeMock);

        boolean result = DiffToolUtils.isEnumContainingType(listTypeMock);

        assertTrue(result);
    }

    @Test
    void testIsEnumContainingTypeWithListNotContainingEnumType() {
        IListType listTypeMock = mock(IListType.class);
        IType nonEnumTypeMock = mock(IType.class);
        when(listTypeMock.getItemType()).thenReturn(nonEnumTypeMock);
        boolean result = DiffToolUtils.isEnumContainingType(listTypeMock);
        assertFalse(result);
    }

    @Test
    void testIsEnumContainingTypeWithNullType() {
        boolean result = DiffToolUtils.isEnumContainingType(null);
        assertFalse(result);
    }

    @Test
    void testIsEnumContainingTypeWithOtherType() {
        IType someOtherTypeMock = mock(IType.class);
        boolean result = DiffToolUtils.isEnumContainingType(someOtherTypeMock);
        assertFalse(result);
    }

    public static Stream<Arguments> testDiffTextValues() {
        return Stream.of(
                Arguments.of(null, null, false, ""),
                Arguments.of(null, "", false, ""),
                Arguments.of("  ", "", false, ""),
                Arguments.of("Same text", "Same text", false, "Same text"),
                Arguments.of("Some text", "Some other text", true, "Some <span style=\"background-color: #CCFFCC;\" " +
                        "id=\"added-diff-0\" previous=\"first-diff\" changeId=\"added-diff-0\" next=\"last-diff\">other </span>text"),
                Arguments.of("Some other text", "Some text", true, "Some <span style=\"background-color: #FDC6C6; text-decoration: line-through;\" " +
                        "id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"last-diff\">other </span>text"),
                Arguments.of("Same text", "Same <span style=\"background-color: red;\">text</span>", false, "Same text"),
                Arguments.of("Same <span style=\"background-color: red;\">text</span>", "Same text", false, "Same text")
        );
    }

    @ParameterizedTest
    @MethodSource("testDiffTextValues")
    void testDiffText(String text1, String text2, boolean expectedDiff, String expectedDiffResult) {
        StringsDiff diff = DiffToolUtils.diffText(text1, text2);
        assertEquals(expectedDiff, diff.isDifferent());
        assertEquals(expectedDiffResult, diff.getResult());
    }

    public static Stream<Arguments> testDiffHtmlValues() {
        return Stream.of(
                Arguments.of(null, null, ""),
                Arguments.of(null, "", ""),
                Arguments.of("  ", "", ""),
                Arguments.of("<h1>Heading</h1><p>Some content for page</p>", "<h1>Heading2</h1><p>Some text for webpage</p>", "<h1>" +
                        "<span style=\"background-color: #FDC6C6; text-decoration: line-through;\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">Heading</span><span style=\"background-color: #CCFFCC;\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"removed-diff-1\">Heading2</span>" +
                        "</h1><p>" +
                        "Some <span style=\"background-color: #FDC6C6; text-decoration: line-through;\" id=\"removed-diff-1\" previous=\"added-diff-0\" changeId=\"removed-diff-1\" next=\"added-diff-1\">content </span><span style=\"background-color: #CCFFCC;\" id=\"added-diff-1\" previous=\"removed-diff-1\" changeId=\"added-diff-1\" next=\"removed-diff-2\">text </span>for <span style=\"background-color: #FDC6C6; text-decoration: line-through;\" id=\"removed-diff-2\" previous=\"added-diff-1\" changeId=\"removed-diff-2\" next=\"added-diff-2\">page</span><span style=\"background-color: #CCFFCC;\" id=\"added-diff-2\" previous=\"removed-diff-2\" changeId=\"added-diff-2\" next=\"last-diff\">webpage</span>" +
                        "</p>")
        );
    }

    @ParameterizedTest
    @MethodSource("testDiffHtmlValues")
    void testDiffHtml(String html1, String html2, String expectedDiff) {
        StringsDiff diff = DiffToolUtils.diffHtml(html1, html2);
        assertEquals(expectedDiff, diff.getResult());
    }

}
