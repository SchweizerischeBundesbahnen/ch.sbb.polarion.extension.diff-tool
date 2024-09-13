package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Data
@Schema(description = "Represents the differences between two WorkItems")
public class WorkItemsDiff {
    @Schema(description = "Left side WorkItem in the comparison", implementation = WorkItem.class)
    private WorkItem leftWorkItem;

    @Schema(description = "Right side WorkItem in the comparison", implementation = WorkItem.class)
    private WorkItem rightWorkItem;

    @JsonIgnore
    private Map<String, FieldDiff> fieldDiffsMap = new HashMap<>();

    public static WorkItemsDiff of(@NotNull WorkItemsPair workItemsPair, Set<String> fieldIds) {
        WorkItemsDiff diff = new WorkItemsDiff();
        diff.leftWorkItem = workItemsPair.getLeftWorkItem();
        diff.rightWorkItem = workItemsPair.getRightWorkItem();

        fieldIds.forEach(fieldId -> {
            FieldDiff fieldDiff = getFieldDiff(workItemsPair.getLeftWorkItem(), workItemsPair.getRightWorkItem(), fieldId);
            if (fieldDiff != null) {
                diff.getFieldDiffsMap().put(fieldId, fieldDiff);
            }
        });

        return diff;
    }

    private static FieldDiff getFieldDiff(@Nullable WorkItem workItemA, @Nullable WorkItem workItemB, @NotNull String fieldId) {
        WorkItem.Field field = Optional.ofNullable(workItemA)
                .map(workItem -> workItem.getField(fieldId))
                .orElse(Optional.ofNullable(workItemB)
                        .map(workItem -> workItem.getField(fieldId))
                        .orElse(null));
        if (field != null) {
            String diffLeft = getDiff(workItemA, fieldId);
            String diffRight = getDiff(workItemB, fieldId);
            if (diffLeft.isEmpty() && diffRight.isEmpty()) {
                return null; // Content in both WorkItems identical, skip this field
            } else {
                return FieldDiff.builder()
                        .id(field.getId())
                        .name(field.getName())
                        .diffLeft(diffLeft)
                        .diffRight(diffRight)
                        .issues(field.issues)
                        .build();
            }
        } else {
            return null; // Shouldn't happen, but in case
        }
    }

    private static String getDiff(@Nullable WorkItem workItemA, @NotNull String fieldId) {
        WorkItem.Field field = Optional.ofNullable(workItemA).map(w -> w.getField(fieldId)).orElse(null);
        return Optional.ofNullable(field).map(WorkItem.Field::getHtmlDiff).orElse("");
    }

    @JsonGetter
    public Collection<FieldDiff> getFieldDiffs() {
        return fieldDiffsMap.values();
    }

    @Data
    @Builder
    @Schema(description = "Represents the differences between fields in WorkItems")
    public static class FieldDiff {
        @Schema(description = "Field ID")
        private String id;

        @Schema(description = "Field name")
        private String name;

        @Schema(description = "Difference on the left side")
        private String diffLeft;

        @Schema(description = "Difference on the right side")
        private String diffRight;

        @Schema(description = "Set of issues related to this field")
        private Set<String> issues;
    }
}
