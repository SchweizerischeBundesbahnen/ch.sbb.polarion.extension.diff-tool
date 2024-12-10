package ch.sbb.polarion.extension.diff_tool.service;

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

import ch.sbb.polarion.extension.diff_tool.util.TestUtils;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.data.model.ICustomField;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void teardown() {
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

    private void assertItemsEqual(int index, String side, WorkItem expected, WorkItem actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null || actual == null) {
            fail(String.format("Items don't conform [%s/%s]: EXPECTED {%s} - ACTUAL {%s}",
                    index, side,
                    expected == null ? "null" : "not null",
                    actual == null ? "null" : "not null"));
        } else if (!(Objects.equals(expected.getOutlineNumber(), actual.getOutlineNumber())
                && Objects.equals(expected.getMovedOutlineNumber(), actual.getMovedOutlineNumber())
                && Objects.equals(expected.getMoveDirection(), actual.getMoveDirection()))) {
            fail(String.format("Items don't conform [%s/%s]: EXPECTED {%s, %s, %s} - ACTUAL {%s, %s, %s}",
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
}
