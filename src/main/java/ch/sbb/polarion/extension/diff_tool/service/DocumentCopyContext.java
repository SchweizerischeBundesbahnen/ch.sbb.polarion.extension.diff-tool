package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.DiffModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DocumentCopyContext extends SettingsAwareMergeContext {

    public DocumentCopyContext(@Nullable String linkRole, @NotNull DiffModel diffModel) {
        super(MergeDirection.LEFT_TO_RIGHT, linkRole == null ? "" : linkRole, diffModel);
    }

}
