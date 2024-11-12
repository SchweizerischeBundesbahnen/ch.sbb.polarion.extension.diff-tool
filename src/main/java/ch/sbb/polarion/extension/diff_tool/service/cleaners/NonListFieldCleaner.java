package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IType;
import org.jetbrains.annotations.NotNull;

public class NonListFieldCleaner implements FieldCleaner {
    @Override
    public void clean(@NotNull IWorkItem workItem, @NotNull WorkItemField field) {
        if (field.isRequired()) {
            if (field.isCustomField()) {
                workItem.setValue(field.getKey(), field.getDefaultValue());
            } else {
                fillStandardFieldDefaultValue(workItem, field);
            }
        } else {
            if (!(workItem.getFieldType(field.getKey()) instanceof IListType)) {
                workItem.setValue(field.getKey(), null);
            }
        }
    }

    public void fillStandardFieldDefaultValue(@NotNull IWorkItem workItem, @NotNull WorkItemField standardField) {
        IType keyType = workItem.getPrototype().getKeyType(standardField.getKey());
        if (keyType instanceof IEnumType) {
            IEnumeration<?> enumeration = workItem.getDataSvc().getEnumerationForKey(workItem.getPrototype().getName(), standardField.getKey(), workItem.getContextId());
            IEnumOption defaultOption = enumeration.getDefaultOption(null);
            if (defaultOption != null) {
                workItem.setValue(standardField.getKey(), defaultOption);
            }
        }
    }
}
