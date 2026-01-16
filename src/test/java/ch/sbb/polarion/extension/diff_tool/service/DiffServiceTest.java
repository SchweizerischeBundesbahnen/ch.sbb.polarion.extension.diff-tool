package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchor;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentContentAnchorsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsContentDiff;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentsFieldsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeMoveDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Project;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairs;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPairsParams;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ImageHandler;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;
import ch.sbb.polarion.extension.diff_tool.util.OutlineNumberComparator;
import ch.sbb.polarion.extension.diff_tool.util.TestUtils;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IRevision;
import com.polarion.platform.persistence.model.ITypedList;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.location.ILocation;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private PolarionService polarionService;

    private DiffService diffService;

    @BeforeEach
    void init() {
        TestUtils.mockDiffSettings();
        diffService = new DiffService(polarionService);
    }

    @AfterEach
    void teardown() {
        TestUtils.clearSettings();
    }

    @Test
    @SneakyThrows
    void testAddDiffClassToImages() {
        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/imageSpansUnprocessed.html");
             InputStream isExpectedResultHtml = this.getClass().getResourceAsStream("/imageSpansExpectedResult.html")) {

            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);

            String processedHtml = new ImageHandler().postProcess(unprocessedHtml, new DiffContext(null, null, "", WorkItemsPairDiffParams.builder().build(), polarionService));
            String expectedHtml = new String(Objects.requireNonNull(isExpectedResultHtml).readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectedHtml, processedHtml);
        }
    }

    @Test
    @SneakyThrows
    void testBrRendering() {
        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/brRendering.html");
             InputStream isExpectedResultHtml = this.getClass().getResourceAsStream("/brRenderingResult.html")) {

            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);

            String processedHtml = DiffToolUtils.computeDiff(Pair.of(unprocessedHtml, ""));
            assertNotNull(processedHtml);
            String expectedHtml = new String(Objects.requireNonNull(isExpectedResultHtml).readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectedHtml, processedHtml);
        }
    }

    @Test
    void testNotEqualButSameVisuallyFields() {
        WorkItem workItemA = mock(WorkItem.class);
        WorkItem workItemB = mock(WorkItem.class);
        WorkItem.Field fieldA = WorkItem.Field.builder()
                .value(new Object())
                .html("<div>same html in both cases</div>")
                .build();
        WorkItem.Field fieldB = WorkItem.Field.builder()
                .value(new Object())
                .html("<div>same html in both cases</div>")
                .build();
        when(workItemA.getField("testField")).thenReturn(fieldA);
        when(workItemB.getField("testField")).thenReturn(fieldB);
        when(workItemA.getProjectId()).thenReturn("someProject");
        when(workItemB.getProjectId()).thenReturn("someProject");

        diffService.fillHtmlDiffs(WorkItemsPair.builder().leftWorkItem(workItemA).rightWorkItem(workItemB).build(), Set.of("testField"), WorkItemsPairDiffParams.builder().build());

        assertNull(fieldA.getHtmlDiff());
        assertNull(fieldB.getHtmlDiff());
    }

    @Test
    void testGetMoveDirection() {
        OutlineNumberComparator outlineNumberComparator = new OutlineNumberComparator();

        IWorkItem leftWorkItemMock = mock(IWorkItem.class);
        IWorkItem rightWorkItemMock = mock(IWorkItem.class);

        WorkItem leftWorkItem = WorkItem.of(leftWorkItemMock, "1", false, false);
        WorkItem rightWorkItem = WorkItem.of(rightWorkItemMock, "1", false, false);
        WorkItemsPair workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        MergeMoveDirection mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), null, null, outlineNumberComparator);

        assertNull(mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), null, null, outlineNumberComparator);

        assertEquals(MergeMoveDirection.DOWN, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "2", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), null, null, outlineNumberComparator);

        assertEquals(MergeMoveDirection.UP, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1.1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), "1", null, outlineNumberComparator);

        assertEquals(MergeMoveDirection.DOWN, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "2", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "1.1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), null, "1", outlineNumberComparator);

        assertEquals(MergeMoveDirection.UP, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1.1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2.1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), "1", "2", outlineNumberComparator);

        assertEquals(MergeMoveDirection.DOWN, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "2.1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "1.1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(), "2", "1", outlineNumberComparator);

        assertEquals(MergeMoveDirection.UP, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1.1-1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2.1-1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();

        WorkItem pairedLeftWorkItem = WorkItem.of(leftWorkItemMock, "1.1", false, false);
        WorkItem pairedRightWorkItem = WorkItem.of(rightWorkItemMock, "2.1", false, false);
        WorkItemsPair pairedWorkItemsPair = WorkItemsPair.builder().leftWorkItem(pairedLeftWorkItem).rightWorkItem(pairedRightWorkItem).build();

        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(pairedWorkItemsPair), "1.1", "2.1", outlineNumberComparator);
        assertNull(mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1.1-1", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2.1-2", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();

        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(pairedWorkItemsPair), "1.1", "2.1", outlineNumberComparator);
        assertEquals(MergeMoveDirection.DOWN, mergeMoveDirection);

        leftWorkItem = WorkItem.of(leftWorkItemMock, "1.1-2", false, false);
        rightWorkItem = WorkItem.of(rightWorkItemMock, "2.1-1", false, false);
        workItemsPair = WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();

        mergeMoveDirection = diffService.getMoveDirection(workItemsPair, List.of(pairedWorkItemsPair), "1.1", "2.1", outlineNumberComparator);
        assertEquals(MergeMoveDirection.UP, mergeMoveDirection);
    }

    @Test
    void testHandleMovedItems() {
        List<WorkItemsPair> pairedWorkItems = new ArrayList<>();
        pairedWorkItems.add(createPair("1", "1"));
        pairedWorkItems.add(createPair("1.1", "3.4"));
        pairedWorkItems.add(createPair("1.2", "1.1"));
        pairedWorkItems.add(createPair("1.2.1", "1.1.1"));
        pairedWorkItems.add(createPair("1.2.1-1", null));
        pairedWorkItems.add(createPair("2", "2"));
        pairedWorkItems.add(createPair("2.1", "3.3"));
        pairedWorkItems.add(createPair("2.1-1", "3.3-1"));
        pairedWorkItems.add(createPair("2.1.1", "2.1"));
        pairedWorkItems.add(createPair("2.1.1-1", "2.1-1"));
        pairedWorkItems.add(createPair("2.2", "2.2"));
        pairedWorkItems.add(createPair("2.2-1", "2.2-1"));
        pairedWorkItems.add(createPair("2.3", null));
        pairedWorkItems.add(createPair("2.4", "2.3"));
        pairedWorkItems.add(createPair("2.4-1", "2.3-1"));
        pairedWorkItems.add(createPair("2.5", "3"));
        pairedWorkItems.add(createPair("2.5-1", "3-1"));
        pairedWorkItems.add(createPair("2.6", "3.1"));
        pairedWorkItems.add(createPair("2.6-1", "3.1-1"));
        pairedWorkItems.add(createPair("2.7", "3.2"));
        pairedWorkItems.add(createPair("2.7-1", "3.2-1"));
        pairedWorkItems.add(createPair("2.8", "2.4"));
        pairedWorkItems.add(createPair("2.8-1", "2.4-1"));
        pairedWorkItems.add(createPair("2.9", "1.1.2"));
        pairedWorkItems.add(createPair("2.9-1", "1.1.2-1"));
        pairedWorkItems.add(createPair("2.10", null));
        pairedWorkItems.add(createPair(null, "3.5"));

        List<WorkItemsPair> movedItemsHandled = diffService.handleMovedItems(pairedWorkItems);

        List<WorkItemsPair> expectedResult = new ArrayList<>();
        expectedResult.add(createPair("1", "1"));
        expectedResult.add(createPair(createWorkItem("1.1", "3.4", MergeMoveDirection.DOWN), createWorkItem("3.4")));
        expectedResult.add(createPair(createWorkItem("1.2"), createWorkItem("1.1", "1.2", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("1.2.1"), createWorkItem("1.1.1", "1.2.1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.9"), createWorkItem("1.1.2", "2.9", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.9-1"), createWorkItem("1.1.2-1", "2.9-1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("1.2", "1.1", MergeMoveDirection.UP), createWorkItem("1.1")));
        expectedResult.add(createPair(createWorkItem("1.2.1", "1.1.1", MergeMoveDirection.UP), createWorkItem("1.1.1")));
        expectedResult.add(createPair("1.2.1-1", null));
        expectedResult.add(createPair("2", "2"));
        expectedResult.add(createPair(createWorkItem("2.1", "3.3", MergeMoveDirection.DOWN), createWorkItem("3.3")));
        expectedResult.add(createPair(createWorkItem("2.1-1", "3.3-1", MergeMoveDirection.DOWN), createWorkItem("3.3-1")));
        expectedResult.add(createPair(createWorkItem("2.1.1"), createWorkItem("2.1", "2.1.1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.1.1-1"), createWorkItem("2.1-1", "2.1.1-1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.1.1", "2.1", MergeMoveDirection.UP), createWorkItem("2.1")));
        expectedResult.add(createPair(createWorkItem("2.1.1-1", "2.1-1", MergeMoveDirection.UP), createWorkItem("2.1-1")));
        expectedResult.add(createPair("2.2", "2.2"));
        expectedResult.add(createPair("2.2-1", "2.2-1"));
        expectedResult.add(createPair("2.3", null));
        expectedResult.add(createPair(createWorkItem("2.4"), createWorkItem("2.3", "2.4", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.4-1"), createWorkItem("2.3-1", "2.4-1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.4", "2.3", MergeMoveDirection.UP), createWorkItem("2.3")));
        expectedResult.add(createPair(createWorkItem("2.4-1", "2.3-1", MergeMoveDirection.UP), createWorkItem("2.3-1")));
        expectedResult.add(createPair(createWorkItem("2.8"), createWorkItem("2.4", "2.8", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.8-1"), createWorkItem("2.4-1", "2.8-1", MergeMoveDirection.DOWN)));
        expectedResult.add(createPair(createWorkItem("2.5", "3", MergeMoveDirection.DOWN), createWorkItem("3")));
        expectedResult.add(createPair(createWorkItem("2.5-1", "3-1", MergeMoveDirection.DOWN), createWorkItem("3-1")));
        expectedResult.add(createPair(createWorkItem("2.6", "3.1", MergeMoveDirection.DOWN), createWorkItem("3.1")));
        expectedResult.add(createPair(createWorkItem("2.6-1", "3.1-1", MergeMoveDirection.DOWN), createWorkItem("3.1-1")));
        expectedResult.add(createPair(createWorkItem("2.7", "3.2", MergeMoveDirection.DOWN), createWorkItem("3.2")));
        expectedResult.add(createPair(createWorkItem("2.7-1", "3.2-1", MergeMoveDirection.DOWN), createWorkItem("3.2-1")));
        expectedResult.add(createPair(createWorkItem("2.8", "2.4", MergeMoveDirection.UP), createWorkItem("2.4")));
        expectedResult.add(createPair(createWorkItem("2.8-1", "2.4-1", MergeMoveDirection.UP), createWorkItem("2.4-1")));
        expectedResult.add(createPair(createWorkItem("2.5"), createWorkItem("3", "2.5", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.5-1"), createWorkItem("3-1", "2.5-1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.6"), createWorkItem("3.1", "2.6", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.6-1"), createWorkItem("3.1-1", "2.6-1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.7"), createWorkItem("3.2", "2.7", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.7-1"), createWorkItem("3.2-1", "2.7-1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.1"), createWorkItem("3.3", "2.1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.1-1"), createWorkItem("3.3-1", "2.1-1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("1.1"), createWorkItem("3.4", "1.1", MergeMoveDirection.UP)));
        expectedResult.add(createPair(createWorkItem("2.9", "1.1.2", MergeMoveDirection.UP), createWorkItem("1.1.2")));
        expectedResult.add(createPair(createWorkItem("2.9-1", "1.1.2-1", MergeMoveDirection.UP), createWorkItem("1.1.2-1")));
        expectedResult.add(createPair("2.10", null));
        expectedResult.add(createPair(null, "3.5"));

        assertEquals(expectedResult.size(), movedItemsHandled.size());

        for (int i = 0; i < expectedResult.size(); i++) {
            WorkItemsPair expectedPair = expectedResult.get(i);
            WorkItemsPair actualPair = movedItemsHandled.get(i);

            assertItemsEqual(i, "LEFT", expectedPair.getLeftWorkItem(), actualPair.getLeftWorkItem());
            assertItemsEqual(i, "RIGHT", expectedPair.getRightWorkItem(), actualPair.getRightWorkItem());
        }
    }

    @Test
    void testGetDocumentsContentDiff() {
        IDataService dataService = mock(IDataService.class);
        IRevision revision = mock(IRevision.class);
        when(dataService.getLastStorageRevision()).thenReturn(revision);

        IModule leftDocument = mock(IModule.class);
        when(leftDocument.getHomePageContent()).thenReturn(Text.html("""
                    <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-1"></h2>
                    <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-2"></h3>
                    <p>Paragraph above</p>
                    <div id="polarion_wiki macro name=module-workitem;params=id=AA-3"></div>
                    <div id="polarion_wiki macro name=module-workitem;params=id=AA-4"></div>
                """));
        ILocation leftDocumentLocation = mock(ILocation.class);
        when(leftDocument.getModuleLocation()).thenReturn(leftDocumentLocation);
        when(leftDocument.getDataSvc()).thenReturn(dataService);

        IModule rightDocument = mock(IModule.class);
        when(rightDocument.getHomePageContent()).thenReturn(Text.html("""
                    <h2 id="polarion_wiki macro name=module-workitem;params=id=AA-5"></h2>
                    <h3 id="polarion_wiki macro name=module-workitem;params=id=AA-6"></h3>
                    <div id="polarion_wiki macro name=module-workitem;params=id=AA-7"></div>
                    <p>Paragraph below</p>
                    <div id="polarion_wiki macro name=module-workitem;params=id=AA-8"></div>
                """));
        ILocation rightDocumentLocation = mock(ILocation.class);
        when(rightDocument.getModuleLocation()).thenReturn(rightDocumentLocation);
        when(rightDocument.getDataSvc()).thenReturn(dataService);

        when(polarionService.getDocumentWithFilledRevision("project1", "space1", "left", "rev1")).thenReturn(leftDocument);
        when(polarionService.getDocumentWithFilledRevision("project1", "space1", "right", "rev1")).thenReturn(rightDocument);

        String linkRole = "link-role";

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(leftDocument.getProject()).thenReturn(trackerProject);
        when(rightDocument.getProject()).thenReturn(trackerProject);

        ILinkRoleOpt linkRoleObjectMock = mock(ILinkRoleOpt.class);
        when(polarionService.getLinkRoleById(linkRole, trackerProject)).thenReturn(linkRoleObjectMock);

        List<WorkItemsPair> pairedWorkItems = new ArrayList<>();
        pairedWorkItems.add(createWorkItemsPair("AA-1", "AA-5"));
        pairedWorkItems.add(createWorkItemsPair("AA-2", "AA-6"));
        pairedWorkItems.add(createWorkItemsPair("AA-3", "AA-7"));
        pairedWorkItems.add(createWorkItemsPair("AA-4", "AA-8"));
        when(polarionService.getPairedWorkItems(leftDocument, rightDocument, linkRoleObjectMock, Collections.emptyList())).thenReturn(pairedWorkItems);

        when(polarionService.renderDocumentContentBlock(leftDocument, "<p>Paragraph above</p>")).thenReturn("<p>Paragraph above</p>");
        when(polarionService.renderDocumentContentBlock(rightDocument, "<p>Paragraph below</p>")).thenReturn("<p>Paragraph below</p>");

        DocumentIdentifier leftDocumentIdentifier = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("left").revision("rev1").build();
        DocumentIdentifier rightDocumentIdentifier = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("right").revision("rev1").build();

        DocumentsContentDiff result = diffService.getDocumentsContentDiff(leftDocumentIdentifier, rightDocumentIdentifier, linkRole);
        assertNotNull(result);
        assertEquals(4, result.getPairedContentAnchors().size());

        List<DocumentContentAnchorsPair> expectedPairedAnchors = new ArrayList<>();
        expectedPairedAnchors.add(DocumentContentAnchorsPair.builder()
                .leftAnchor(DocumentContentAnchor.builder().id("AA-1").contentAbove("").contentBelow("").build())
                .rightAnchor(DocumentContentAnchor.builder().id("AA-5").contentAbove("").contentBelow("").build())
                .build());

        expectedPairedAnchors.add(DocumentContentAnchorsPair.builder()
                .leftAnchor(DocumentContentAnchor.builder().id("AA-2").contentAbove("").contentBelow("").build())
                .rightAnchor(DocumentContentAnchor.builder().id("AA-6").contentAbove("").contentBelow("").build())
                .build());

        expectedPairedAnchors.add(DocumentContentAnchorsPair.builder()
                .leftAnchor(DocumentContentAnchor.builder().id("AA-3").contentAbove("<p>Paragraph above</p>").contentBelow("")
                        .diffAbove("<p><span class=\"diff-html-removed\" id=\"added-diff-0\" previous=\"first-diff\" changeId=\"added-diff-0\" next=\"last-diff\">Paragraph above</span></p>").build())
                .rightAnchor(DocumentContentAnchor.builder().id("AA-7").contentAbove("").contentBelow("")
                        .diffAbove("<p><span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"last-diff\">Paragraph above</span></p>").build())
                .build());

        expectedPairedAnchors.add(DocumentContentAnchorsPair.builder()
                .leftAnchor(DocumentContentAnchor.builder().id("AA-4").contentAbove("").contentBelow("")
                        .diffAbove("<p><span class=\"diff-html-added\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"last-diff\">Paragraph below</span></p>").build())
                .rightAnchor(DocumentContentAnchor.builder().id("AA-8").contentAbove("<p>Paragraph below</p>").contentBelow("")
                        .diffAbove("<p><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"first-diff\" changeId=\"added-diff-0\" next=\"last-diff\">Paragraph below</span></p>").build())
                .build());

        for (int i = 0; i < expectedPairedAnchors.size(); i++) {
            DocumentContentAnchorsPair expectedPairedAnchor = expectedPairedAnchors.get(i);
            DocumentContentAnchorsPair actualPairedAnchor = result.getPairedContentAnchors().get(i);

            assertAnchorsEqual(i, "LEFT", expectedPairedAnchor.getLeftAnchor(), actualPairedAnchor.getLeftAnchor());
            assertAnchorsEqual(i, "RIGHT", expectedPairedAnchor.getRightAnchor(), actualPairedAnchor.getRightAnchor());
        }
    }

    @Test
    void testGetFieldsDiff() {
        IModule left = mock(IModule.class);
        IModule right = mock(IModule.class);

        when(left.getCustomFieldsList()).thenReturn(List.of("C1", "C2", "C3", "L1", "L2", "L3"));
        when(right.getCustomFieldsList()).thenReturn(List.of("R1", "R2", "R3", "C1", "C2", "C3"));

        when(left.getFieldLabel(anyString())).thenAnswer((Answer<String>) i -> (String) i.getArguments()[0]);
        when(right.getFieldLabel(anyString())).thenAnswer((Answer<String>) i -> (String) i.getArguments()[0]);

        when(right.getCustomFieldPrototype(anyString())).thenReturn(mock(ICustomField.class));
        when(left.getCustomFieldPrototype(anyString())).thenReturn(mock(ICustomField.class));

        when(left.getProjectId()).thenReturn("projectId");

        when(polarionService.getFieldValue(any(), anyString(), any())).thenAnswer((Answer<String>) i -> (String) i.getArguments()[1]);

        Collection<DocumentsFieldsPair> result = diffService.getFieldsDiff(left, right, false, true);
        assertEquals(3, result.size());

        result = diffService.getFieldsDiff(left, right, false, false);
        assertEquals(9, result.size());
    }

    @Test
    void testFindWorkItemsPairs() {
        String leftProjectId = "left-project";
        String leftProjectName = "Left project";
        String rightProjectId = "right-project";
        String rightProjectName = "Right project";
        List<String> leftWorkItemIds = List.of("LP-1", "LP-2", "LP-3", "LP-4", "LP-5");
        String linkRole = "link-role";

        ITrackerProject leftTrackerProjectMock = mock(ITrackerProject.class);
        when(polarionService.getTrackerProject(leftProjectId)).thenReturn(leftTrackerProjectMock);

        ILinkRoleOpt linkRoleObjectMock = mock(ILinkRoleOpt.class);
        when(polarionService.getLinkRoleById(linkRole, leftTrackerProjectMock)).thenReturn(linkRoleObjectMock);

        IProject leftProjectMock = mock(IProject.class);
        when(leftProjectMock.getId()).thenReturn(leftProjectId);
        when(leftProjectMock.getName()).thenReturn(leftProjectName);
        when(polarionService.getProject(leftProjectId)).thenReturn(leftProjectMock);
        when(polarionService.userAuthorizedForMerge(leftProjectId)).thenReturn(true);

        IProject rightProjectMock = mock(IProject.class);
        when(rightProjectMock.getId()).thenReturn(rightProjectId);
        when(rightProjectMock.getName()).thenReturn(rightProjectName);
        when(polarionService.getProject(rightProjectId)).thenReturn(rightProjectMock);
        when(polarionService.userAuthorizedForMerge(rightProjectId)).thenReturn(false);

        IWorkItem lp1Mock = mock(IWorkItem.class);
        when(lp1Mock.getId()).thenReturn("LP-1");
        IWorkItem lp2Mock = mock(IWorkItem.class);
        when(lp2Mock.getId()).thenReturn("LP-2");
        IWorkItem lp3Mock = mock(IWorkItem.class);
        when(lp3Mock.getId()).thenReturn("LP-3");
        IWorkItem lp4Mock = mock(IWorkItem.class);
        when(lp4Mock.getId()).thenReturn("LP-4");
        IWorkItem lp5Mock = mock(IWorkItem.class);
        when(lp5Mock.getId()).thenReturn("LP-5");
        List<IWorkItem> leftWorkItems = List.of(lp1Mock, lp2Mock, lp3Mock, lp4Mock, lp5Mock);
        when(polarionService.getWorkItems(leftProjectId, leftWorkItemIds)).thenReturn(leftWorkItems);

        IWorkItem rp1Mock = mock(IWorkItem.class);
        when(rp1Mock.getId()).thenReturn("RP-1");
        when(polarionService.getPairedWorkItems(lp1Mock, rightProjectId, linkRole)).thenReturn(List.of(rp1Mock));

        when(polarionService.getPairedWorkItems(lp2Mock, rightProjectId, linkRole)).thenReturn(Collections.emptyList());
        when(polarionService.getPairedWorkItems(lp3Mock, rightProjectId, linkRole)).thenReturn(Collections.emptyList());

        IWorkItem rp2Mock = mock(IWorkItem.class);
        IWorkItem rp3Mock = mock(IWorkItem.class);
        when(polarionService.getPairedWorkItems(lp4Mock, rightProjectId, linkRole)).thenReturn(List.of(rp2Mock, rp3Mock));

        IWorkItem rp4Mock = mock(IWorkItem.class);
        when(rp4Mock.getId()).thenReturn("RP-4");
        when(polarionService.getPairedWorkItems(lp5Mock, rightProjectId, linkRole)).thenReturn(List.of(rp4Mock));

        WorkItemsPairs realResult = diffService.findWorkItemsPairs(new WorkItemsPairsParams(leftProjectId, rightProjectId, leftWorkItemIds, linkRole));

        Collection<WorkItemsPair> pairedWorkItems = new ArrayList<>();
        pairedWorkItems.add(WorkItemsPair.of(lp1Mock, rp1Mock));
        pairedWorkItems.add(WorkItemsPair.of(lp2Mock, null));
        pairedWorkItems.add(WorkItemsPair.of(lp3Mock, null));
        pairedWorkItems.add(WorkItemsPair.of(lp5Mock, rp4Mock));

        WorkItemsPairs expectedResult = WorkItemsPairs.builder()
                .leftProject(Project.builder()
                        .id(leftProjectId)
                        .name(leftProjectName)
                        .authorizedForMerge(true)
                        .build())
                .rightProject(Project.builder()
                        .id(rightProjectId)
                        .name(rightProjectName)
                        .authorizedForMerge(false)
                        .build())
                .pairedWorkItems(pairedWorkItems)
                .leftWorkItemIdsWithRedundancy(List.of("LP-4"))
                .build();

        assertEquals(expectedResult, realResult);
    }

    @Test
    void testBuildWorkItem() {
        // Mock necessary objects
        IWorkItem mockWorkItem = mock(IWorkItem.class);
        Set<String> fieldIds = Set.of("description", "status");

        // Basic work item properties setup
        String workItemId = "TEST-123";
        String title = "Test Work Item";
        String projectId = "TestProject";
        IContextId contextId = mock(IContextId.class);

        when(mockWorkItem.getId()).thenReturn(workItemId);
        when(mockWorkItem.getTitle()).thenReturn(title);
        when(mockWorkItem.getProjectId()).thenReturn(projectId);
        when(mockWorkItem.getRevision()).thenReturn("123");

        when(mockWorkItem.getContextId()).thenReturn(contextId);

        // Mock polarionService.getGeneralFields and polarionService.getCustomFields
        FieldMetadata statusMetadata = createMockFieldMetadata("status");
        when(polarionService.getGeneralFields(anyString(), any())).thenReturn(Set.of(statusMetadata));
        FieldMetadata customFieldMetadata = createMockFieldMetadata("customField");
        when(polarionService.getCustomFields(anyString(), any(), isNull())).thenReturn(Set.of(customFieldMetadata));

        // Test with basic parameters
        WorkItem result = diffService.buildWorkItem(mockWorkItem, fieldIds, null, false, null, false);

        // Verify basic properties
        assertNotNull(result);
        assertEquals(workItemId, result.getId());
        assertEquals(title, result.getTitle());
        assertEquals(projectId, result.getProjectId());
    }

    private FieldMetadata createMockFieldMetadata(String fieldId) {
        FieldMetadata metadata = mock(FieldMetadata.class);
        when(metadata.getId()).thenReturn(fieldId);
        lenient().when(metadata.getType()).thenReturn(mock(IEnumType.class));
        return metadata;
    }

    @Test
    void testGetFieldValueWithEnumIdComparisonMode() {
        WorkItem workItemMock = mock(WorkItem.class);
        IEnumType fieldTypeMock = mock(IEnumType.class);
        IEnumOption enumOptionMock = mock(IEnumOption.class);
        when(polarionService.getFieldValue(any(), any(), any())).thenReturn(enumOptionMock);
        when(enumOptionMock.getId()).thenReturn("enumId");

        Object result = diffService.getFieldValue(null, "someEnumField", workItemMock, fieldTypeMock, true);

        assertEquals("enumId", result);
    }

    @Test
    void testGetFieldValueWithEnumListComparisonMode() {
        WorkItem workItemMock = mock(WorkItem.class);
        IEnumType fieldTypeMock = mock(IEnumType.class);
        ITypedList typedListMock = mock(ITypedList.class);

        String fieldId = "someEnumListField";
        List<IEnumOption> enumList = List.of(mock(IEnumOption.class), mock(IEnumOption.class));
        when(polarionService.getFieldValue(any(), anyString(), any())).thenReturn(typedListMock);
        when(typedListMock.stream()).thenReturn(enumList.stream());
        when(enumList.get(0).getId()).thenReturn("enumId");
        when(enumList.get(1).getId()).thenReturn("enumId");

        Object result = diffService.getFieldValue(null, fieldId, workItemMock, fieldTypeMock, true);

        assertEquals(List.of("enumId", "enumId"), result);
    }

    // ==================== getIndex() method tests ====================

    @Test
    void testGetIndex_emptyList_returnsMinusOne() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        WorkItemsPair surrogate = createPair("1", "2");

        int result = diffService.getIndex(surrogate, new ArrayList<>(), comparator, DiffService.DiffSide.RIGHT);

        assertEquals(-1, result);
    }

    @Test
    void testGetIndex_surrogateGreaterThanAllItems_returnsMinusOne() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("2", "2"));

        WorkItemsPair surrogate = createPair("3", "3");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        assertEquals(-1, result);
    }

    @Test
    void testGetIndex_surrogateLessThanFirstItem_returnsOne() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("2", "2"));
        pairs.add(createPair("3", "3"));

        WorkItemsPair surrogate = createPair("1", "1");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        assertEquals(1, result);
    }

    @Test
    void testGetIndex_surrogateInMiddle_returnsCorrectIndex() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("3", "3"));
        pairs.add(createPair("5", "5"));

        WorkItemsPair surrogate = createPair("2", "2");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        assertEquals(1, result);
    }

    @Test
    void testGetIndex_withLeftSide_usesLeftItemAsBase() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "5"));
        pairs.add(createPair("3", "2"));

        // Surrogate with right outline "2.5" - when using LEFT side, base is leftItem
        WorkItemsPair surrogate = createPair("2", "2.5");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.LEFT);

        // With LEFT side, baseItem is leftItem: "1" then "3"
        // Surrogate right is "2.5", comparing with baseItem (left): "1" < "2.5", "3" > "2.5"
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_inlinedItemsUpdateIndex() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("1-1", "1-1")); // Inlined under "1"
        pairs.add(createPair("1-2", "1-2")); // Inlined under "1"
        pairs.add(createPair("3", "3"));

        WorkItemsPair surrogate = createPair("2", "2");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Surrogate "2" should be placed after the inlined items but before "3"
        assertEquals(3, result);
    }

    @Test
    void testGetIndex_surrogateIsInlinedAndCurrentIsNot_returnsCurrentIndex() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("2", "2"));

        // Surrogate is an inline item of "1"
        WorkItemsPair surrogate = createPair("1-1", "1-1");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Surrogate "1-1" is inlined under indexed "1", current "2" is not inlined
        // Should return index of "2" which is 1
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_bothCurrentAndSurrogateAreInlined_surrogateSmaller_returnsCurrentIndex() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("1-2", "1-2")); // Inlined under "1"
        pairs.add(createPair("1-3", "1-3")); // Inlined under "1"

        // Surrogate is also inlined under "1" but smaller than "1-2"
        WorkItemsPair surrogate = createPair("1-1", "1-1");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Both current "1-2" and surrogate "1-1" are inlined under "1"
        // Since "1-2" > "1-1", should return index 1
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_bothCurrentAndSurrogateAreInlined_surrogateLarger_continuesLoop() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("1-1", "1-1")); // Inlined under "1"
        pairs.add(createPair("1-2", "1-2")); // Inlined under "1"
        pairs.add(createPair("2", "2"));

        // Surrogate is also inlined under "1" but larger than existing inlines
        WorkItemsPair surrogate = createPair("1-3", "1-3");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Surrogate "1-3" > all inlined items, should be placed before "2"
        assertEquals(3, result);
    }

    @Test
    void testGetIndex_pairedItemHasMovedOutlineNumber_andGreaterThanSurrogate_returnsIndex() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));

        // Create pair where pairedItem (left when side=RIGHT) has movedOutlineNumber
        WorkItem leftItem = createWorkItem("3", "1.5", MergeMoveDirection.UP);
        WorkItem rightItem = createWorkItem("2");
        pairs.add(createPair(leftItem, rightItem));

        WorkItemsPair surrogate = createPair("1.5", "1.5");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // pairedItem (leftItem) has movedOutlineNumber and outline "3" > "1.5"
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_pairedItemHasMovedOutlineNumber_andSmallerThanSurrogate_continuesLoop() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));

        // Create pair where pairedItem has movedOutlineNumber but is smaller than surrogate
        WorkItem leftItem = createWorkItem("1.5", "1", MergeMoveDirection.DOWN);
        WorkItem rightItem = createWorkItem("2");
        pairs.add(createPair(leftItem, rightItem));

        pairs.add(createPair("4", "4"));

        WorkItemsPair surrogate = createPair("3", "3");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // pairedItem outline "1.5" < "3", else-if doesn't match so loop continues without updating index
        // At i=2, "4" > "3" triggers return index+1 = 0+1 = 1
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_baseItemIsNull_skipsComparison() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("2", null)); // Right item (baseItem for RIGHT side) is null
        pairs.add(createPair("4", "4"));

        WorkItemsPair surrogate = createPair("3", "3");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Index 1 has null baseItem (right), skips comparison without updating index
        // At i=2, "4" > "3" triggers return index+1 = 0+1 = 1
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_baseItemIsInlineNotStructural_skipsComparison() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("2", "1-1")); // Right item contains "-", so not structural
        pairs.add(createPair("4", "4"));

        WorkItemsPair surrogate = createPair("3", "3");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // Index 1 has non-structural baseItem "1-1", should skip and continue to index 2
        assertEquals(2, result);
    }

    @Test
    void testGetIndex_pairedItemIsNull_comparesWithBaseItem() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair(null, "3")); // Left item (pairedItem for RIGHT side) is null

        WorkItemsPair surrogate = createPair("2", "2");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        // pairedItem is null, so condition (pairedItem == null || ...) is true
        // baseItem "3" > surrogate "2", returns index + 1 = 1
        assertEquals(1, result);
    }

    @Test
    void testGetIndex_complexScenarioWithMultipleInlinedItems() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("1-1", "1-1"));
        pairs.add(createPair("1-2", "1-2"));
        pairs.add(createPair("2", "2"));
        pairs.add(createPair("2-1", "2-1"));
        pairs.add(createPair("3", "3"));

        // Surrogate should be placed between "2-1" and "3"
        WorkItemsPair surrogate = createPair("2.5", "2.5");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.RIGHT);

        assertEquals(5, result);
    }

    @Test
    void testGetIndex_leftSideWithNullPairedItem() {
        OutlineNumberComparator comparator = new OutlineNumberComparator();
        List<WorkItemsPair> pairs = new ArrayList<>();
        pairs.add(createPair("1", "1"));
        pairs.add(createPair("3", null)); // Right item is null, which is pairedItem for LEFT side

        WorkItemsPair surrogate = createPair("2", "2");

        int result = diffService.getIndex(surrogate, pairs, comparator, DiffService.DiffSide.LEFT);

        // Using LEFT side: baseItem is leftItem "3", pairedItem is rightItem (null)
        // Since pairedItem is null, compares baseItem "3" > surrogate right "2"
        assertEquals(1, result);
    }

    private void assertItemsEqual(int index, String side, WorkItem expected, WorkItem actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null || actual == null) {
            fail(String.format("Items don't match [%s/%s]: EXPECTED {%s} - ACTUAL {%s}",
                    index, side,
                    expected == null ? "null" : "not null",
                    actual == null ? "null" : "not null"));
        } else if (!(Objects.equals(expected.getOutlineNumber(), actual.getOutlineNumber())
                && Objects.equals(expected.getMovedOutlineNumber(), actual.getMovedOutlineNumber())
                && Objects.equals(expected.getMoveDirection(), actual.getMoveDirection()))) {
            fail(String.format("Items don't match [%s/%s]: EXPECTED {%s, %s, %s} - ACTUAL {%s, %s, %s}",
                    index, side,
                    expected.getOutlineNumber(), expected.getMovedOutlineNumber(), expected.getMoveDirection(),
                    actual.getOutlineNumber(), actual.getMovedOutlineNumber(), actual.getMoveDirection()));
        }
    }

    private WorkItemsPair createPair(String leftOutlineNumber, String rightOutlineNumber) {
        return createPair(leftOutlineNumber != null ? createWorkItem(leftOutlineNumber) : null, rightOutlineNumber != null ? createWorkItem(rightOutlineNumber) : null);
    }

    private WorkItemsPair createPair(WorkItem leftWorkItem, WorkItem rightWorkItem) {
        WorkItemsPair.WorkItemsPairBuilder builder = WorkItemsPair.builder();
        if (leftWorkItem != null) {
            builder.leftWorkItem(leftWorkItem);
        }
        if (rightWorkItem != null) {
            builder.rightWorkItem(rightWorkItem).build();
        }
        return builder.build();
    }

    private WorkItem createWorkItem(String outlineNumber) {
        return createWorkItem(outlineNumber, null, null);
    }

    private WorkItem createWorkItem(String outlineNumber, String movedOutlineNumber, MergeMoveDirection moveDirection) {
        IWorkItem underlyingObject = mock(IWorkItem.class);
        WorkItem workItem = WorkItem.of(underlyingObject, outlineNumber, false, false);
        workItem.setMovedOutlineNumber(movedOutlineNumber);
        workItem.setMoveDirection(moveDirection);
        return workItem;
    }

    private WorkItemsPair createWorkItemsPair(String leftWorkItemId, String rightWorkItemId) {
        WorkItem leftWorkItem = WorkItem.builder().id(leftWorkItemId).build();
        WorkItem rightWorkItem = WorkItem.builder().id(rightWorkItemId).build();
        return WorkItemsPair.builder().leftWorkItem(leftWorkItem).rightWorkItem(rightWorkItem).build();
    }

    private void assertAnchorsEqual(int index, String side, DocumentContentAnchor expected, DocumentContentAnchor actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null || actual == null) {
            fail(String.format("Anchors don't match [%s/%s]: EXPECTED {%s} - ACTUAL {%s}",
                    index, side,
                    expected == null ? "null" : "not null",
                    actual == null ? "null" : "not null"));
        } else if (!(Objects.equals(expected.getId(), actual.getId()))) {
            fail(String.format("Anchors IDs don't match [%s/%s]: EXPECTED {%s} - ACTUAL {%s}",
                    index, side, expected.getId(), actual.getId()));
        } else if (!(Objects.equals(expected.getContentAbove(), actual.getContentAbove())
                && Objects.equals(expected.getContentBelow(), actual.getContentBelow()))) {
            fail(String.format("Anchors content doesn't match [%s/%s]: EXPECTED {%s, %s} - ACTUAL {%s, %s}",
                    index, side,
                    expected.getContentAbove(), expected.getContentBelow(),
                    actual.getContentAbove(), actual.getContentBelow()));
        } else if (!(Objects.equals(expected.getDiffAbove(), actual.getDiffAbove())
                && Objects.equals(expected.getDiffBelow(), actual.getDiffBelow()))) {
            fail(String.format("Anchors diff doesn't match [%s/%s]: EXPECTED {%s, %s} - ACTUAL {%s, %s}",
                    index, side,
                    expected.getDiffAbove(), expected.getDiffBelow(),
                    actual.getDiffAbove(), actual.getDiffBelow()));
        }
    }


}
