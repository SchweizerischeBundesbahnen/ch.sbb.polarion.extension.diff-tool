package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
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

    public List<MergeReportEntry> getCreationFailed() {
        return getEntriesByType(OperationResultType.CREATION_FAILED);
    }

    public List<MergeReportEntry> getCreated() {
        return getEntriesByType(OperationResultType.CREATED);
    }

    public List<MergeReportEntry> getCopied() {
        return getEntriesByType(OperationResultType.COPIED);
    }

    public List<MergeReportEntry> getDeleted() {
        return getEntriesByType(OperationResultType.DELETED);
    }

    public List<MergeReportEntry> getDuplicateSkipped() {
        return getEntriesByType(OperationResultType.DUPLICATE_SKIPPED);
    }

    public List<MergeReportEntry> getModified() {
        return getEntriesByType(OperationResultType.MODIFIED);
    }

    public List<MergeReportEntry> getMoved() {
        return getEntriesByType(OperationResultType.MOVED);
    }

    public List<MergeReportEntry> getMoveFailed() {
        return getEntriesByType(OperationResultType.MOVE_FAILED);
    }

    public List<MergeReportEntry> getMoveSkipped() {
        return getEntriesByType(OperationResultType.MOVE_SKIPPED);
    }

    public List<MergeReportEntry> getMergeDetached() {
        return getEntriesByType(OperationResultType.DETACHED);
    }

    public List<MergeReportEntry> getWarnings() {
        return getEntriesByType(OperationResultType.WARNING);
    }

    public String getLogs() {
        return entries.stream()
                .sorted(Comparator.comparing(entry -> entry.getOperationTime().format(DATE_TIME_FORMATTER)))
                .map(this::formatLogEntry)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatLogEntry(MergeReportEntry entry) {
        return String.format("%s: '%s' -- %s -- %s",
                entry.getOperationTime().format(DATE_TIME_FORMATTER),
                entry.getOperationResultType(),
                getEntityInfo(entry),
                entry.getDescription());
    }

    private String getEntityInfo(MergeReportEntry entry) {
        if (entry.getWorkItemsPair() != null) {
            return "left WI '%s', right WI '%s'".formatted(
                    Optional.ofNullable(entry.getWorkItemsPair().getLeftWorkItem()).map(WorkItem::getId).orElse("null"),
                    Optional.ofNullable(entry.getWorkItemsPair().getRightWorkItem()).map(WorkItem::getId).orElse("null"));
        } else if (entry.getFieldId() != null) {
            return "field ID '%s'".formatted(entry.getFieldId());
        } else if (entry.getDocumentsContentPair() != null) {
            return "left WI '%s', right WI '%s'".formatted(
                    entry.getDocumentsContentPair().getLeftWorkItemId(),
                    entry.getDocumentsContentPair().getRightWorkItemId());
        } else {
            return "UNKNOWN";
        }
    }

    public void addEntry(MergeReportEntry entry) {
        entries.add(entry);
    }

    public enum OperationResultType {
        CONFLICTED,
        COPIED,
        CREATED,
        CREATION_FAILED,
        DELETED,
        DETACHED,
        DUPLICATE_SKIPPED,
        MODIFIED,
        MOVED,
        MOVE_SKIPPED,
        MOVE_FAILED,
        MOVED_FROM,
        PROHIBITED,
        WARNING
    }

}
