package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.report.MergeReport;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class MergeContext {
    protected final MergeDirection direction;
    protected final MergeReport mergeReport = new MergeReport();
    protected final boolean updateAttachmentReferences;

    protected MergeContext(@NotNull MergeDirection direction, boolean updateAttachmentReferences) {
        this.direction = direction;
        this.updateAttachmentReferences = updateAttachmentReferences;
    }
}
