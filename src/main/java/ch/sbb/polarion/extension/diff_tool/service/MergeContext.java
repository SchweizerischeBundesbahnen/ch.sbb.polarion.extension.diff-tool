package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.report.MergeReportEntry;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItem;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemsPair;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import com.polarion.alm.tracker.model.IWorkItem;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class MergeContext {
    @Getter
    final MergeDirection direction;
    @Getter
    final MergeReport mergeReport = new MergeReport();
    final String linkRole;
    @Getter
    final DiffModel diffModel;

    protected MergeContext(@NotNull MergeDirection direction, @NotNull String linkRole, DiffModel diffModel) {
        this.direction = direction;
        this.linkRole = linkRole;
        this.diffModel = diffModel;
    }

    public WorkItem getSourceWorkItem(WorkItemsPair pair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? pair.getLeftWorkItem() : pair.getRightWorkItem();
    }

    public WorkItem getTargetWorkItem(WorkItemsPair pair) {
        return direction == MergeDirection.LEFT_TO_RIGHT ? pair.getRightWorkItem() : pair.getLeftWorkItem();
    }

    public void putTargetWorkItem(WorkItemsPair pair, IWorkItem targetWorkItem) {
        if (direction == MergeDirection.LEFT_TO_RIGHT) {
            pair.setRightWorkItem(WorkItem.of(targetWorkItem));
        } else {
            pair.setLeftWorkItem(WorkItem.of(targetWorkItem));
        }
    }

    public void reportEntry(@NotNull MergeReport.OperationResultType operationResultType, @NotNull WorkItemsPair workItemsPair, @NotNull String description) {
        mergeReport.addEntry(new MergeReportEntry(operationResultType, workItemsPair, description));
    }

}
