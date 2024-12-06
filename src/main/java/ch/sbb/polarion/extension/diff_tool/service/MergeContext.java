package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class MergeContext {
    protected final MergeDirection direction;
    protected final MergeReport mergeReport = new MergeReport();

    protected MergeContext(@NotNull MergeDirection direction) {
        this.direction = direction;
    }
}
