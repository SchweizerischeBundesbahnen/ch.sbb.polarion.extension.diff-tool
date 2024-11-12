package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.data.model.IListType;
import org.jetbrains.annotations.NotNull;

public class ListFieldCleaner implements FieldCleaner {
    @Override
    public void clean(@NotNull IWorkItem workItem, @NotNull WorkItemField field) {
        if (workItem.getFieldType(field.getKey()) instanceof IListType) {
            ListCleaner cleaner = ListCleanerProvider.getInstance(field.getKey());
            if (!field.isRequired() && cleaner != null) {
                cleaner.clean(workItem);
            }
        }
    }
}
