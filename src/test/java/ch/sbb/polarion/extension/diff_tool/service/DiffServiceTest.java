package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeMoveDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsDiffParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.service.handler.DiffContext;
import ch.sbb.polarion.extension.diff_tool.service.handler.impl.ImageHandler;
import ch.sbb.polarion.extension.diff_tool.util.DiffToolUtils;

import com.polarion.alm.tracker.model.IWorkItem;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffServiceTest {

    @Mock
    private PolarionService polarionService;

    private DiffService diffService;

    @BeforeEach
    void init() {
        diffService = new DiffService(polarionService);
    }

    @Test
    @SneakyThrows
    void testAddDiffClassToImages() {
        try (InputStream isUnprocessedHtml = this.getClass().getResourceAsStream("/imageSpansUnprocessed.html");
             InputStream isExpectedResultHtml = this.getClass().getResourceAsStream("/imageSpansExpectedResult.html")) {

            String unprocessedHtml = new String(Objects.requireNonNull(isUnprocessedHtml).readAllBytes(), StandardCharsets.UTF_8);

            String processedHtml = new ImageHandler().postProcess(unprocessedHtml, new DiffContext(null, null, "", WorkItemsDiffParams.builder().build(), polarionService));
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

        diffService.fillHtmlDiffs(WorkItemsPair.builder().leftWorkItem(workItemA).rightWorkItem(workItemB).build(), Set.of("testField"),
                WorkItemsDiffParams.builder().build());

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
