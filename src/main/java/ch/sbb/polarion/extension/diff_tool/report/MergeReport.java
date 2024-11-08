package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MergeReport {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Schema(description = "List of log entries that describe the merge operations and their results", implementation = MergeReportEntry.class)
    private final List<MergeReportEntry> entries;

    public MergeReport() {
        entries = new ArrayList<>();
    }

    public List<MergeReportEntry> getEntriesByType(OperationResultType operationResultType) {
        return entries.stream()
                .filter(entry -> entry.getOperationResultType() == operationResultType)
                .toList();
    }

    public List<MergeReportEntry> getConflicted() {
        return getEntriesByType(OperationResultType.CONFLICTED);
    }

    public List<MergeReportEntry> getProhibited() {
        return getEntriesByType(OperationResultType.PROHIBITED);
    }

    public List<MergeReportEntry> getNotPaired() {
        return getEntriesByType(OperationResultType.NOT_PAIRED);
    }

    public List<MergeReportEntry> getCreated() {
        return getEntriesByType(OperationResultType.CREATED);
    }

    public List<MergeReportEntry> getDeleted() {
        return getEntriesByType(OperationResultType.DELETED);
    }

    public List<MergeReportEntry> getModified() {
        return getEntriesByType(OperationResultType.MODIFIED);
    }

    public List<MergeReportEntry> getMoved() {
        return getEntriesByType(OperationResultType.MOVED);
    }

    public List<MergeReportEntry> getNotMoved() {
        return getEntriesByType(OperationResultType.NOT_MOVED);
    }

    public String getLogs() {
        return entries.stream()
                .sorted(Comparator.comparing(entry -> entry.getOperationTime().format(DATE_TIME_FORMATTER)))
                .map(this::formatLogEntry)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatLogEntry(MergeReportEntry entry) {
        WorkItem leftWorkItem = entry.getWorkItemsPair().getLeftWorkItem();
        String leftWorkItemId = leftWorkItem == null ? "null" : leftWorkItem.getId();
        WorkItem rightWorkItem = entry.getWorkItemsPair().getRightWorkItem();
        String rightWorkItemId = rightWorkItem == null ? "null" : rightWorkItem.getId();

        return String.format("%s: '%s' -- left WI '%s', right WI '%s' -- %s",
                entry.getOperationTime().format(DATE_TIME_FORMATTER),
                entry.getOperationResultType(),
                leftWorkItemId,
                rightWorkItemId,
                entry.getDescription());
    }

    public void addEntry(MergeReportEntry entry) {
        entries.add(entry);
    }

    public enum OperationResultType {
        DUPLICATED,
        CONFLICTED,
        CREATED,
        DELETED,
        MODIFIED,
        MOVED,
        MOVE_SKIPPED,
        NOT_MOVED,
        NOT_PAIRED,
        PROHIBITED
    }

}
