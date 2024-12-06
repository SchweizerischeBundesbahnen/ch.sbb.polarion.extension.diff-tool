package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Input data to compare WorkItems")
public class WorkItemsPairDiffParams<T extends ProjectWorkItem> {
    @Schema(description = "ID of the left project")
    private String leftProjectId; // This value is needed to load diff-configuration (see configName attribute below) even if leftWorkItem is not available

    @Schema(description = "Left WorkItem used in the comparison", implementation = ProjectWorkItem.class)
    private T leftWorkItem;

    @Schema(description = "Right WorkItem used in the comparison", implementation = ProjectWorkItem.class)
    private T rightWorkItem;

    @Schema(description = "Link role between paired WorkItems")
    private String pairedWorkItemsLinkRole;

    @Schema(description = "Indicates if the paired WorkItems differ")
    private Boolean pairedWorkItemsDiffer;

    @Schema(description = "Indicates if enums must be compared by their ID")
    private Boolean compareEnumsById;

    @Schema(description = "Name of the configuration used for comparison")
    private String configName;

    @Schema(description = "Cache bucket Id")
    private String configCacheBucketId;

    public boolean isPairedWorkItemsDiffer() {
        return Boolean.TRUE.equals(pairedWorkItemsDiffer);
    }

    public boolean isCompareEnumsById() {
        return Boolean.TRUE.equals(compareEnumsById);
    }
}
