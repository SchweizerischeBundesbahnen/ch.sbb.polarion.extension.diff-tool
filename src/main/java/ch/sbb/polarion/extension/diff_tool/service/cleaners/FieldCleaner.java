package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;

public interface FieldCleaner {
    void clean(@NotNull IWorkItem workItem, @NotNull WorkItemField field);
}
