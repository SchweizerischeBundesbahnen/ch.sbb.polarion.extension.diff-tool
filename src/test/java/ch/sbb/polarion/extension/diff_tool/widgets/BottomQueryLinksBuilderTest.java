package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.api.utils.links.PortalLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BottomQueryLinksBuilderTest {

    @Mock
    private RichPageWidgetCommonContext context;

    @Mock
    private DataSet dataSet;

    @Mock
    private SharedLocalization localization;

    @BeforeEach
    void setUp() {
        lenient().when(context.localization()).thenReturn(localization);
    }

    @Test
    void testWithShortMessages() {
        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        BottomQueryLinksBuilder result = builder.withShortMessages();
        assertThat(result).isSameAs(builder);
    }

    @Test
    void testRenderSetsFooterClassName() {
        when(dataSet.size()).thenReturn(5);
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(attributesBuilder);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        // renderCounts - div
        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder queryDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(tagSelector.div()).thenReturn(countsDiv).thenReturn(queryDiv);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(attributesBuilder).className("polarion-rpw-table-footer");
    }

    @Test
    void testRenderCountsWithLinkWhenSizeLessOrEqualTopItems() {
        PortalLink link = mock(PortalLink.class);
        when(dataSet.openInTableLink()).thenReturn(link);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString("form.modules.label.showMulti.item", "5")).thenReturn("Showing 5 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        // renderCounts creates a div
        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        // Link present -> wraps in <a>
        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        HtmlTagSelector<HtmlTagBuilder> countsDivTagSelector = mock(HtmlTagSelector.class);
        when(countsDivContent.tag()).thenReturn(countsDivTagSelector);

        HtmlTagBuilder aTag = mock(HtmlTagBuilder.class);
        when(countsDivTagSelector.a()).thenReturn(aTag);

        HtmlAttributesBuilder aAttributes = mock(HtmlAttributesBuilder.class);
        when(aTag.attributes()).thenReturn(aAttributes);
        when(aAttributes.href(link)).thenReturn(aAttributes);
        when(aAttributes.target("_top")).thenReturn(aAttributes);

        HtmlContentBuilder aContent = mock(HtmlContentBuilder.class);
        when(aTag.append()).thenReturn(aContent);

        // renderOpenInTable - also creates a div (not in collection scope, and link is present)
        HtmlTagBuilder openInTableDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(contentTagSelector.div()).thenReturn(countsDiv).thenReturn(openInTableDiv);

        // Re-setup contentBuilder.tag() to return selector with divs for both renderCounts and renderOpenInTable
        // Actually need to handle the second call to contentBuilder.tag().div()
        // Let me restructure - contentTagSelector.div() is called multiple times

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(countsDivAttributes).className("polarion-rpw-table-counts");
        verify(aAttributes).href(link);
        verify(aAttributes).target("_top");
        verify(aContent).text("Showing 5 items");
    }

    @Test
    void testRenderCountsWithoutLinkWhenSizeGreaterThanTopItems() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(25);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString("form.modules.label.showMultiOf.item", "10", "25")).thenReturn("Showing 10 of 25 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(countsDivAttributes).className("polarion-rpw-table-counts");
        verify(countsDivContent).text("Showing 10 of 25 items");
    }

    @Test
    void testRenderShortCountsMessage() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(15);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString("richpages.widget.kanbanBoard.showMoreThenTopItems", "10", "15")).thenReturn("10 of 15");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.withShortMessages().render(tagBuilder);

        verify(countsDivContent).text("10 of 15");
    }

    @Test
    void testRenderInCollectionScopeSkipsOpenInTable() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(3);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 3 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        // Use BaselineCollection - should skip renderOpenInTable
        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.BaselineCollection, dataSet, 10);
        builder.render(tagBuilder);

        // openInTableLink is only called once (in renderCounts), not a second time for renderOpenInTable
        verify(dataSet, times(1)).openInTableLink();
    }

    @Test
    void testRenderOpenInTableWithLink() {
        PortalLink link = mock(PortalLink.class);
        when(dataSet.openInTableLink()).thenReturn(link);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString("form.modules.label.showMulti.item", "5")).thenReturn("Showing 5 items");
        when(localization.getString("macro.general.openInTable")).thenReturn("Open in Table");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        // renderCounts div
        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        // renderOpenInTable div
        HtmlTagBuilder openInTableDiv = mock(HtmlTagBuilder.class);

        when(contentTagSelector.div()).thenReturn(countsDiv).thenReturn(openInTableDiv);

        // renderOpenInTable: div attributes
        HtmlAttributesBuilder openInTableDivAttributes = mock(HtmlAttributesBuilder.class);
        when(openInTableDiv.attributes()).thenReturn(openInTableDivAttributes);

        // renderOpenInTable: div > a
        HtmlContentBuilder openInTableDivContent = mock(HtmlContentBuilder.class);
        when(openInTableDiv.append()).thenReturn(openInTableDivContent);

        HtmlTagSelector<HtmlTagBuilder> openInTableDivTagSelector = mock(HtmlTagSelector.class);
        when(openInTableDivContent.tag()).thenReturn(openInTableDivTagSelector);

        HtmlTagBuilder aTag = mock(HtmlTagBuilder.class);
        when(openInTableDivTagSelector.a()).thenReturn(aTag);

        HtmlAttributesBuilder aAttributes = mock(HtmlAttributesBuilder.class);
        when(aTag.attributes()).thenReturn(aAttributes);
        when(aAttributes.href(link)).thenReturn(aAttributes);
        when(aAttributes.target("_blank")).thenReturn(aAttributes);

        // renderOpenInTable: a > img
        HtmlContentBuilder aContent = mock(HtmlContentBuilder.class);
        when(aTag.append()).thenReturn(aContent);

        HtmlTagSelector<HtmlTagBuilder> aTagSelector = mock(HtmlTagSelector.class);
        when(aContent.tag()).thenReturn(aTagSelector);

        HtmlTagBuilder imgTag = mock(HtmlTagBuilder.class);
        when(aTagSelector.img()).thenReturn(imgTag);

        HtmlAttributesBuilder imgAttributes = mock(HtmlAttributesBuilder.class);
        when(imgTag.attributes()).thenReturn(imgAttributes);
        when(imgAttributes.src(any())).thenReturn(imgAttributes);
        when(imgAttributes.title("Open in Table")).thenReturn(imgAttributes);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(openInTableDivAttributes).className("polarion-rpw-table-open-in-table");
        verify(aAttributes).href(link);
        verify(aAttributes).target("_blank");
        verify(imgAttributes).title("Open in Table");
    }

    @Test
    void testRenderOpenInTableWithNullLink() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        // openInTableLink called twice: once in renderCounts, once in renderOpenInTable
        verify(dataSet, times(2)).openInTableLink();
        // Since link is null, no "open-in-table" div is created - only the counts div
        verify(countsDivAttributes).className("polarion-rpw-table-counts");
    }

    @Test
    void testRenderShowQueryWhenNotPdfAndQueryPresent() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn("type:req AND status:approved");
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(context.generateUniqueElementId()).thenReturn("unique-id-42");
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");
        when(localization.getString("macro.general.showQuery")).thenReturn("Show Query");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        // renderCounts div
        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        // renderShowQuery first div (show-query)
        HtmlTagBuilder showQueryDiv = mock(HtmlTagBuilder.class);
        // renderShowQuery second div (query content)
        HtmlTagBuilder queryContentDiv = mock(HtmlTagBuilder.class);

        when(contentTagSelector.div()).thenReturn(countsDiv).thenReturn(showQueryDiv).thenReturn(queryContentDiv);

        // countsDiv setup
        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);
        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        // showQueryDiv setup
        HtmlAttributesBuilder showQueryDivAttributes = mock(HtmlAttributesBuilder.class);
        when(showQueryDiv.attributes()).thenReturn(showQueryDivAttributes);

        HtmlContentBuilder showQueryDivContent = mock(HtmlContentBuilder.class);
        when(showQueryDiv.append()).thenReturn(showQueryDivContent);

        HtmlTagSelector<HtmlTagBuilder> showQueryDivTagSelector = mock(HtmlTagSelector.class);
        when(showQueryDivContent.tag()).thenReturn(showQueryDivTagSelector);

        HtmlTagBuilder imgTag = mock(HtmlTagBuilder.class);
        when(showQueryDivTagSelector.img()).thenReturn(imgTag);

        HtmlAttributesBuilder imgAttributes = mock(HtmlAttributesBuilder.class);
        when(imgTag.attributes()).thenReturn(imgAttributes);
        when(imgAttributes.src(any())).thenReturn(imgAttributes);
        when(imgAttributes.title("Show Query")).thenReturn(imgAttributes);
        when(imgAttributes.onClick(anyString())).thenReturn(imgAttributes);

        // queryContentDiv setup
        HtmlAttributesBuilder queryContentDivAttributes = mock(HtmlAttributesBuilder.class);
        when(queryContentDiv.attributes()).thenReturn(queryContentDivAttributes);
        when(queryContentDivAttributes.id("unique-id-42")).thenReturn(queryContentDivAttributes);
        when(queryContentDivAttributes.className("polarion-rpw-table-query")).thenReturn(queryContentDivAttributes);

        HtmlContentBuilder queryContentDivContent = mock(HtmlContentBuilder.class);
        when(queryContentDiv.append()).thenReturn(queryContentDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(showQueryDivAttributes).className("polarion-rpw-table-show-query");
        verify(imgAttributes).title("Show Query");
        verify(imgAttributes).onClick("var style = getElementById('unique-id-42').style; style.display = (style.display == 'block') ? 'none' : 'block';");
        verify(queryContentDivAttributes).id("unique-id-42");
        verify(queryContentDivAttributes).className("polarion-rpw-table-query");
        verify(queryContentDivContent).textWithFormatting("type:req AND status:approved");
    }

    @Test
    void testRenderShowQuerySkippedWhenPdf() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(5);
        when(context.target()).thenReturn(RichTextRenderTarget.PDF_EXPORT);
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        // queryToShow should never be called when isPdf is true
        verify(dataSet, never()).queryToShow();
    }

    @Test
    void testRenderShowQuerySkippedWhenQueryEmpty() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn("   ");
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        // generateUniqueElementId should not be called when query is empty/whitespace
        verify(context, never()).generateUniqueElementId();
    }

    @Test
    void testRenderCountsExactlyAtTopItems() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(10);
        when(dataSet.queryToShow()).thenReturn(null);
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(localization.getString("form.modules.label.showMulti.item", "10")).thenReturn("Showing 10 items");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv);

        HtmlAttributesBuilder countsDivAttributes = mock(HtmlAttributesBuilder.class);
        when(countsDiv.attributes()).thenReturn(countsDivAttributes);

        HtmlContentBuilder countsDivContent = mock(HtmlContentBuilder.class);
        when(countsDiv.append()).thenReturn(countsDivContent);

        // topItems == size, so should use showMulti (not showMultiOf)
        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 10);
        builder.render(tagBuilder);

        verify(localization).getString("form.modules.label.showMulti.item", "10");
        verify(localization, never()).getString(eq("form.modules.label.showMultiOf.item"), anyString(), anyString());
    }

    @Test
    void testRenderFullScenarioNotInCollectionScope() {
        PortalLink link = mock(PortalLink.class);
        when(dataSet.openInTableLink()).thenReturn(link);
        when(dataSet.size()).thenReturn(30);
        when(dataSet.queryToShow()).thenReturn("type:req");
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(context.generateUniqueElementId()).thenReturn("uid-1");
        when(localization.getString("form.modules.label.showMultiOf.item", "20", "30")).thenReturn("Showing 20 of 30 items");
        when(localization.getString("macro.general.openInTable")).thenReturn("Open in Table");
        when(localization.getString("macro.general.showQuery")).thenReturn("Show Query");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        // 4 divs: counts, openInTable, showQuery, queryContent
        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder openInTableDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder showQueryDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder queryContentDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv).thenReturn(openInTableDiv).thenReturn(showQueryDiv).thenReturn(queryContentDiv);

        // queryContentDiv has chained attributes: .attributes().id(uid).className(...)
        HtmlAttributesBuilder queryContentDivAttributes = mock(HtmlAttributesBuilder.class);
        when(queryContentDiv.attributes()).thenReturn(queryContentDivAttributes);
        when(queryContentDivAttributes.id("uid-1")).thenReturn(queryContentDivAttributes);
        HtmlContentBuilder queryContentDivContent = mock(HtmlContentBuilder.class);
        when(queryContentDiv.append()).thenReturn(queryContentDivContent);

        // Not in collection scope (WorkItem), so all three sections render
        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.WorkItem, dataSet, 20);
        builder.render(tagBuilder);

        // Verify all sections were rendered
        verify(countsDiv.attributes()).className("polarion-rpw-table-counts");
        verify(openInTableDiv.attributes()).className("polarion-rpw-table-open-in-table");
        verify(showQueryDiv.attributes()).className("polarion-rpw-table-show-query");
        verify(queryContentDivAttributes).id("uid-1");
        verify(queryContentDivAttributes).className("polarion-rpw-table-query");
    }

    @Test
    void testRenderInCollectionScopeSkipsOpenInTableButRendersShowQuery() {
        when(dataSet.openInTableLink()).thenReturn(null);
        when(dataSet.size()).thenReturn(5);
        when(dataSet.queryToShow()).thenReturn("type:req");
        when(context.target()).thenReturn(RichTextRenderTarget.RP_VIEW);
        when(context.generateUniqueElementId()).thenReturn("uid-2");
        when(localization.getString(eq("form.modules.label.showMulti.item"), anyString())).thenReturn("Showing 5 items");
        when(localization.getString("macro.general.showQuery")).thenReturn("Show Query");

        HtmlTagBuilder tagBuilder = mock(HtmlTagBuilder.class);
        HtmlAttributesBuilder tagAttributes = mock(HtmlAttributesBuilder.class);
        when(tagBuilder.attributes()).thenReturn(tagAttributes);

        HtmlContentBuilder contentBuilder = mock(HtmlContentBuilder.class);
        when(tagBuilder.append()).thenReturn(contentBuilder);

        HtmlTagSelector<HtmlTagBuilder> contentTagSelector = mock(HtmlTagSelector.class);
        when(contentBuilder.tag()).thenReturn(contentTagSelector);

        // 3 divs: counts, showQuery, queryContent (no openInTable for collection scope)
        HtmlTagBuilder countsDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder showQueryDiv = mock(HtmlTagBuilder.class, Answers.RETURNS_DEEP_STUBS);
        HtmlTagBuilder queryContentDiv = mock(HtmlTagBuilder.class);
        when(contentTagSelector.div()).thenReturn(countsDiv).thenReturn(showQueryDiv).thenReturn(queryContentDiv);

        // queryContentDiv has chained attributes: .attributes().id(uid).className(...)
        HtmlAttributesBuilder queryContentDivAttributes = mock(HtmlAttributesBuilder.class);
        when(queryContentDiv.attributes()).thenReturn(queryContentDivAttributes);
        when(queryContentDivAttributes.id("uid-2")).thenReturn(queryContentDivAttributes);
        HtmlContentBuilder queryContentDivContent = mock(HtmlContentBuilder.class);
        when(queryContentDiv.append()).thenReturn(queryContentDivContent);

        // BaselineCollection scope - should skip renderOpenInTable
        BottomQueryLinksBuilder builder = new BottomQueryLinksBuilder(context, PrototypeEnum.BaselineCollection, dataSet, 10);
        builder.render(tagBuilder);

        verify(countsDiv.attributes()).className("polarion-rpw-table-counts");
        verify(showQueryDiv.attributes()).className("polarion-rpw-table-show-query");
        verify(queryContentDivAttributes).id("uid-2");
        verify(queryContentDivAttributes).className("polarion-rpw-table-query");
        // openInTableLink should only be called once (in renderCounts), not for renderOpenInTable
        verify(dataSet, times(1)).openInTableLink();
    }
}
