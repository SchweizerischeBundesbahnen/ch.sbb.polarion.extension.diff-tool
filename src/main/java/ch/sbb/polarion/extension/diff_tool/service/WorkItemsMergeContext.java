package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Project;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import org.jetbrains.annotations.NotNull;

public class WorkItemsMergeContext extends MergeContext {

    final Project leftProject;
    final Project rightProject;

    public WorkItemsMergeContext(@NotNull Project leftProject, @NotNull Project rightProject, @NotNull MergeDirection direction, @NotNull String linkRole, DiffModel diffModel) {
        super(direction, linkRole, diffModel);
        this.leftProject = leftProject;
        this.rightProject = rightProject;
    }

    public Project getSourceProject() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? leftProject : rightProject;
    }

    public Project getTargetProject() {
        return direction == MergeDirection.LEFT_TO_RIGHT ? rightProject : leftProject;
    }
}
