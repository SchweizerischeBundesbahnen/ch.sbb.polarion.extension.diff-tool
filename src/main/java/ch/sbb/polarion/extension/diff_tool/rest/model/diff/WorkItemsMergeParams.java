package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Work items merge input data")
public class WorkItemsMergeParams extends MergeParams {
    @Schema(description = "Left project data", implementation = Project.class)
    private Project leftProject;

    @Schema(description = "Right project data", implementation = Project.class)
    private Project rightProject;

    public WorkItemsMergeParams(Project leftProject, Project rightProject, MergeDirection direction, String linkRole,
                                String configName, String configCacheBucketId, List<WorkItemsPair> pairs) {
        super(direction, linkRole, configName, configCacheBucketId, pairs);
        this.leftProject = leftProject;
        this.rightProject = rightProject;
    }
}
