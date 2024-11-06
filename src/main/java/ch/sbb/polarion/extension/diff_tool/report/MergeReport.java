package ch.sbb.polarion.extension.diff_tool.report;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class MergeReport {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final List<MergeReportEntry> conflicted;
    private final List<MergeReportEntry> prohibited;
    private final List<MergeReportEntry> notPaired;
    private final List<MergeReportEntry> created;
    private final List<MergeReportEntry> modified;
    private final List<MergeReportEntry> moved;
    private final List<MergeReportEntry> notMoved;

    public MergeReport() {
        conflicted = new ArrayList<>();
        prohibited = new ArrayList<>();
        notPaired = new ArrayList<>();
        created = new ArrayList<>();
        modified = new ArrayList<>();
        moved = new ArrayList<>();
        notMoved = new ArrayList<>();
    }

    public String getLogs() {
        return Stream.of(
                        conflicted.stream().map(entry -> formatLogEntry(entry, "CONFLICTED")),
                        prohibited.stream().map(entry -> formatLogEntry(entry, "PROHIBITED")),
                        notPaired.stream().map(entry -> formatLogEntry(entry, "NOT_PAIRED")),
                        created.stream().map(entry -> formatLogEntry(entry, "CREATED")),
                        modified.stream().map(entry -> formatLogEntry(entry, "MODIFIED")),
                        moved.stream().map(entry -> formatLogEntry(entry, "MOVED")),
                        notMoved.stream().map(entry -> formatLogEntry(entry, "NOT_MOVED"))
                )
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(entry -> entry.split(": ")[0]))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatLogEntry(MergeReportEntry entry, String action) {
        WorkItem leftWorkItem = entry.getWorkItemsPair().getLeftWorkItem();
        String leftWorkItemId = leftWorkItem == null ? "null" : leftWorkItem.getId();
        WorkItem rightWorkItem = entry.getWorkItemsPair().getRightWorkItem();
        String rightWorkItemId = rightWorkItem == null ? "null" : rightWorkItem.getId();
        return String.format("%s: '%s' -- left WI '%s', right WI '%s' -- %s",
                entry.getOperationTime().format(DATE_TIME_FORMATTER),
                action,
                leftWorkItemId,
                rightWorkItemId,
                entry.getDescription());
    }

}
