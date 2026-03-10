package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.polarion.alm.tracker.model.IWorkItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Work items pair to be merged")
public class MergeWorkItemsPair extends WorkItemsPair {

    @Schema(description = "List of fields to be merged", implementation = MergeField.class)
    private List<MergeField> fieldsToMerge;

    public MergeWorkItemsPair(@NotNull IWorkItem leftWorkItem, @Nullable IWorkItem rightWorkItem, @NotNull List<MergeField> fieldsToMerge) {
        super(WorkItem.of(leftWorkItem), WorkItem.of(rightWorkItem));
        this.fieldsToMerge = fieldsToMerge;
    }

    public MergeWorkItemsPair(@NotNull WorkItem leftWorkItem, @Nullable WorkItem rightWorkItem, @NotNull List<MergeField> fieldsToMerge) {
        super(leftWorkItem, rightWorkItem);
        this.fieldsToMerge = fieldsToMerge;
    }

    public boolean fieldSelectedForMerge(@NotNull DiffField diffField) {
        if (fieldsToMerge == null || fieldsToMerge.isEmpty()) {
            return true; // No explicit field selection means all fields are selected
        }
        MergeField mergeField = fieldsToMerge.stream().filter(f -> f.getId().equals(diffField.getKey())).findFirst().orElse(null);
        return mergeField != null && mergeField.isSelected();
    }
}
