package ch.sbb.polarion.extension.diff_tool.service.handler.impl;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.util.TestUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinksHandlerTest {

    @BeforeEach
    void init() {
        TestUtils.mockDiffSettings();
    }

    @AfterEach
    void teardown() {
        TestUtils.clearSettings();
    }

    @Test
    @SneakyThrows
    void testAppendPairedWorkItemIdUsingRevision() {

        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/pairedWorkItem.html");
             InputStream isExpectedResultHtml = this.getClass().getResourceAsStream("/pairedWorkItemResult.html")) {

            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(Objects.requireNonNull(isExpectedResultHtml).readAllBytes(), StandardCharsets.UTF_8);

            String projectId = "elibrary";

            PolarionService polarionService = mock(PolarionService.class);

            IWorkItem workItemLastRevision = mock(IWorkItem.class);
            when(polarionService.getWorkItem(eq(projectId), eq("EL-615"), isNull())).thenReturn(workItemLastRevision);

            IWorkItem linkedWorkItemLastRevision = mock(IWorkItem.class);
            when(linkedWorkItemLastRevision.getId()).thenReturn("EL-740");
            when(polarionService.getPairedWorkItems(eq(workItemLastRevision), eq(projectId), anyString())).thenReturn(Collections.singletonList(linkedWorkItemLastRevision));

            // let's assume that the same workitem but some specific revision returns another linked item
            IWorkItem workItemSpecificRevision = mock(IWorkItem.class);
            when(polarionService.getWorkItem(eq(projectId), eq("EL-615"), eq("766"))).thenReturn(workItemSpecificRevision); //NOSONAR

            IWorkItem linkedWorkItemSpecificRevision = mock(IWorkItem.class);
            when(linkedWorkItemSpecificRevision.getId()).thenReturn("EL-745");
            when(polarionService.getPairedWorkItems(eq(workItemSpecificRevision), eq(projectId), anyString())).thenReturn(Collections.singletonList(linkedWorkItemSpecificRevision));

            String result = new LinksHandler().appendPairedWorkItemId(unprocessedHtml,
                    projectId, projectId, new DiffContext(WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false), WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false),
                            "testFieldId", WorkItemsPairDiffParams.builder().pairedWorkItemsLinkRole("roleId").build(), polarionService));

            assertEquals(expectedHtml, result);
        }
    }

    @Test
    @SneakyThrows
    void testAppendNotExistingPairedWorkItemId() {

        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/pairedWorkItem.html")) {
            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);
            String projectId = "elibrary";

            PolarionService polarionService = mock(PolarionService.class);

            IWorkItem workItemLastRevision = mock(IWorkItem.class);
            when(polarionService.getWorkItem(eq(projectId), eq("EL-615"), isNull())).thenReturn(workItemLastRevision);

            String result = new LinksHandler().appendPairedWorkItemId(unprocessedHtml,
                    projectId, projectId, new DiffContext(WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false), WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false),
                            "testFieldId", WorkItemsPairDiffParams.builder().pairedWorkItemsLinkRole("roleId").build(), polarionService));

            assertEquals(unprocessedHtml, result);
        }
    }

    @Test
    void testAppendPairedWorkItemId_minimalRequiredAttributes() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-456");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-456\""));
    }

    @Test
    void testAppendPairedWorkItemId_attributesInDifferentOrder_classFirst() {
        String html = "<span class=\"polarion-rte-link\" data-item-id=\"WI-123\" data-type=\"workItem\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-456");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-456\""));
    }

    @Test
    void testAppendPairedWorkItemId_attributesInDifferentOrder_dataTypeFirst() {
        String html = "<span data-type=\"workItem\" class=\"polarion-rte-link\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-456");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-456\""));
    }

    @Test
    void testAppendPairedWorkItemId_attributesInDifferentOrder_dataItemIdFirst() {
        String html = "<span data-item-id=\"WI-123\" data-type=\"workItem\" class=\"polarion-rte-link\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-456");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-456\""));
    }

    @Test
    void testAppendPairedWorkItemId_withDataScope() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\" data-scope=\"externalProject\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("externalProject"), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-789");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-789\""));
    }

    @Test
    void testAppendPairedWorkItemId_withDataScopeInDifferentPosition() {
        String html = "<span data-scope=\"externalProject\" class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("externalProject"), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-789");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-789\""));
    }

    @Test
    void testAppendPairedWorkItemId_withDataRevision() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\" data-revision=\"100\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), eq("100"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-REV");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-REV\""));
    }

    @Test
    void testAppendPairedWorkItemId_withDataRevisionInDifferentPosition() {
        String html = "<span data-revision=\"100\" data-type=\"workItem\" class=\"polarion-rte-link\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), eq("100"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-REV");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-REV\""));
    }

    @Test
    void testAppendPairedWorkItemId_withBothDataScopeAndDataRevision() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\" data-scope=\"externalProject\" data-revision=\"200\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("externalProject"), eq("WI-123"), eq("200"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-BOTH");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-BOTH\""));
    }

    @Test
    void testAppendPairedWorkItemId_withBothDataScopeAndDataRevisionReversedOrder() {
        String html = "<span data-revision=\"200\" data-scope=\"externalProject\" data-item-id=\"WI-123\" data-type=\"workItem\" class=\"polarion-rte-link\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("externalProject"), eq("WI-123"), eq("200"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-BOTH");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-BOTH\""));
    }

    @Test
    void testAppendPairedWorkItemId_withAdditionalAttributes() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\" data-option-id=\"long\" title=\"Some title\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-EXTRA");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-EXTRA\""));
        assertTrue(result.contains("data-option-id=\"long\""));
        assertTrue(result.contains("title=\"Some title\""));
    }

    @Test
    void testAppendPairedWorkItemId_withAdditionalAttributesMixed() {
        String html = "<span title=\"Some title\" data-type=\"workItem\" data-option-id=\"long\" class=\"polarion-rte-link\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-MIXED");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-MIXED\""));
    }

    @Test
    void testAppendPairedWorkItemId_multipleLinks() {
        String html = "Text <span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-1\">link1</span> middle " +
                "<span data-item-id=\"WI-2\" class=\"polarion-rte-link\" data-type=\"workItem\">link2</span> end";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        IWorkItem workItem1 = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-1"), isNull())).thenReturn(workItem1);
        IWorkItem pairedWorkItem1 = mock(IWorkItem.class);
        when(pairedWorkItem1.getId()).thenReturn("PAIRED-1");
        when(polarionService.getPairedWorkItems(eq(workItem1), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem1));

        IWorkItem workItem2 = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-2"), isNull())).thenReturn(workItem2);
        IWorkItem pairedWorkItem2 = mock(IWorkItem.class);
        when(pairedWorkItem2.getId()).thenReturn("PAIRED-2");
        when(polarionService.getPairedWorkItems(eq(workItem2), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem2));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-1\""));
        assertTrue(result.contains("data-paired-item-id=\"PAIRED-2\""));
    }

    @Test
    void testAppendPairedWorkItemId_noMatchingLinks() {
        String html = "<span class=\"other-class\" data-type=\"workItem\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_missingDataType() {
        String html = "<span class=\"polarion-rte-link\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_missingDataItemId() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_wrongDataType() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"hyperlink\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_emptyHtml() {
        String html = "";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_noLinks() {
        String html = "<p>Some text without any links</p>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_workItemNotFound() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-NOTFOUND\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-NOTFOUND"), isNull())).thenThrow(new RuntimeException("Work item not found"));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_noPairedWorkItem() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\">content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.emptyList());

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertEquals(html, result);
    }

    @Test
    void testAppendPairedWorkItemId_allAttributesRandomOrder1() {
        String html = "<span data-scope=\"proj\" data-type=\"workItem\" data-revision=\"50\" class=\"polarion-rte-link\" data-item-id=\"WI-ALL\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("proj"), eq("WI-ALL"), eq("50"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-ALL");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-ALL\""));
    }

    @Test
    void testAppendPairedWorkItemId_allAttributesRandomOrder2() {
        String html = "<span data-item-id=\"WI-ALL\" data-revision=\"50\" class=\"polarion-rte-link\" data-scope=\"proj\" data-type=\"workItem\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq("proj"), eq("WI-ALL"), eq("50"))).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-ALL2");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-ALL2\""));
    }

    @Test
    void testAppendPairedWorkItemId_mixedLinksOnlyOneMatches() {
        String html = "Start <span class=\"other-class\" data-type=\"workItem\" data-item-id=\"WI-1\">not matched</span> " +
                "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-2\">matched</span> end";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);

        IWorkItem workItem2 = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-2"), isNull())).thenReturn(workItem2);
        IWorkItem pairedWorkItem2 = mock(IWorkItem.class);
        when(pairedWorkItem2.getId()).thenReturn("PAIRED-2");
        when(polarionService.getPairedWorkItems(eq(workItem2), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem2));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-2\""));
        assertTrue(result.contains("<span class=\"other-class\" data-type=\"workItem\" data-item-id=\"WI-1\">not matched</span>"));
    }

    @Test
    void testAppendPairedWorkItemId_withWhitespaceInAttributes() {
        String html = "<span  class=\"polarion-rte-link\"   data-type=\"workItem\"  data-item-id=\"WI-123\" >content</span>";
        String projectId = "testProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        when(polarionService.getWorkItem(eq(projectId), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-WS");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(projectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, projectId, projectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-WS\""));
    }

    @Test
    void testAppendPairedWorkItemId_dataScopeUsedOverSourceProject() {
        String html = "<span class=\"polarion-rte-link\" data-type=\"workItem\" data-item-id=\"WI-123\" data-scope=\"overrideProject\">content</span>";
        String sourceProjectId = "testProject";
        String targetProjectId = "targetProject";

        PolarionService polarionService = mock(PolarionService.class);
        IWorkItem workItem = mock(IWorkItem.class);
        // Should use "overrideProject" from data-scope, not "testProject"
        when(polarionService.getWorkItem(eq("overrideProject"), eq("WI-123"), isNull())).thenReturn(workItem);

        IWorkItem pairedWorkItem = mock(IWorkItem.class);
        when(pairedWorkItem.getId()).thenReturn("PAIRED-SCOPE");
        when(polarionService.getPairedWorkItems(eq(workItem), eq(targetProjectId), anyString())).thenReturn(Collections.singletonList(pairedWorkItem));

        String result = new LinksHandler().appendPairedWorkItemId(html, sourceProjectId, targetProjectId,
                createContext(polarionService));

        assertTrue(result.contains("data-paired-item-id=\"PAIRED-SCOPE\""));
    }

    private DiffContext createContext(PolarionService polarionService) {
        return new DiffContext(
                WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false),
                WorkItem.of(mock(IWorkItem.class), "wi_outline_number", false, false),
                "testFieldId",
                WorkItemsPairDiffParams.builder().pairedWorkItemsLinkRole("roleId").build(),
                polarionService);
    }
}
