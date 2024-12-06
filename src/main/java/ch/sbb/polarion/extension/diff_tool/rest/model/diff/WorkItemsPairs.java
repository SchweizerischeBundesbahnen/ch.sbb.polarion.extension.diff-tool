package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@Schema(description = "Represents starting point of diffing multiple work items - their project names and pairs of work items to be diffed")
public class WorkItemsPairs {

    @Schema(description = "The left project information")
    private Project leftProject;

    @Schema(description = "The right project information")
    private Project rightProject;

    @Schema(description = "A collection of paired WorkItems to be diffed", implementation = WorkItemsPair.class)
    private Collection<WorkItemsPair> pairedWorkItems;

    @Schema(description = "A collection of WorkItem IDs from left project which contain more than 1 counterpart WorkItem from right project and certain link role", implementation = WorkItemsPair.class)
    private Collection<String> leftWorkItemIdsWithRedundancy;

}
