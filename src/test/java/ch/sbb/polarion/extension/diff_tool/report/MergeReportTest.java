package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeReportTest {

    private MergeReport mergeReport;

    @BeforeEach
    public void setUp() {
        mergeReport = new MergeReport();

        mergeReport.getConflicted().add(createEntry("AA-123", "BB-456", "Conflict occurred", LocalDateTime.of(2023, 10, 5, 10, 0)));
        mergeReport.getProhibited().add(createEntry("AA-789", "BB-012", "Prohibited action", LocalDateTime.of(2023, 10, 5, 11, 0)));
        mergeReport.getCreated().add(createEntry("AA-345", "BB-678", "Created item", LocalDateTime.of(2023, 10, 5, 12, 0)));
    }

    @Test
    public void testGetEntriesAsSingleString() {
        String expectedOutput = String.join(System.lineSeparator(),
                "2023-10-05 10:00:00.000: 'CONFLICTED' -- left WI 'AA-123', right WI 'BB-456' -- Conflict occurred",
                "2023-10-05 11:00:00.000: 'PROHIBITED' -- left WI 'AA-789', right WI 'BB-012' -- Prohibited action",
                "2023-10-05 12:00:00.000: 'CREATED' -- left WI 'AA-345', right WI 'BB-678' -- Created item"
        );

        String actualOutput = mergeReport.getLogs();
        assertEquals(expectedOutput, actualOutput);
    }

    private MergeReportEntry createEntry(String leftId, String rightId, String description, LocalDateTime operationTime) {
        WorkItem left = new WorkItem();
        left.setId(leftId);
        WorkItem right = new WorkItem();
        right.setId(rightId);
        WorkItemsPair workItemsPair = new WorkItemsPair(left, right);
        return new MergeReportEntry(workItemsPair, description, operationTime);
    }
}
