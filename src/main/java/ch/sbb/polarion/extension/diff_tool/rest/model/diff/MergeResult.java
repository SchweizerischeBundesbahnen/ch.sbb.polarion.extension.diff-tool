package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a merge operation")
public class MergeResult {

    @Schema(description = "Indicates whether the merge operation was successful")
    private boolean success;

    @Schema(description = "Indicates whether the target module has structural changes")
    private boolean targetModuleHasStructuralChanges;

    @Schema(description = "Indicates whether the merge was not authorized")
    private boolean mergeNotAuthorized;

    @Builder.Default
    @Schema(description = "List of WorkItem pairs that were created during the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> createdPairs = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItem pairs that were modified during the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> modifiedPairs = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItem pairs that caused conflicts during the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> conflictedPairs = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItem pairs which logically are prohibited to be merged", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> prohibitedPairs = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItems from source document in one project which do not have counterparts in another project of target document", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> notPaired = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItem pairs that were moved during the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> movedPairs = new ArrayList<>();

    @Builder.Default
    @Schema(description = "List of WorkItem pairs that were not moved during the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> notMovedPairs = new ArrayList<>();
}
