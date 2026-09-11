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
@Schema(description = "Summary of a chapter merge operation")
public class ChapterMergeInfo {

    @Schema(description = "Outline number the merged chapter got in the target document, eg. '3.2'")
    private String insertedOutlineNumber;

    @Schema(description = "IDs of work items created in the target document")
    @Builder.Default
    private List<String> createdWorkItemIds = new ArrayList<>();

    @Schema(description = "IDs of work items moved into the target document")
    @Builder.Default
    private List<String> movedWorkItemIds = new ArrayList<>();

    @Schema(description = "IDs of work items referenced in the target document")
    @Builder.Default
    private List<String> referencedWorkItemIds = new ArrayList<>();

    @Schema(description = "IDs of work item types whose layout was copied into the target document")
    @Builder.Default
    private List<String> copiedLayoutTypeIds = new ArrayList<>();
}
