package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Merge input data")
public class MergeParams {

    @Schema(description = "Identifier for the left document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier leftDocument;

    @Schema(description = "Identifier for the right document", implementation = DocumentIdentifier.class)
    private DocumentIdentifier rightDocument;

    @Schema(description = "The direction of the merge operation", implementation = MergeDirection.class)
    private MergeDirection direction;

    @Schema(description = "The role of the link connecting the two documents")
    private String linkRole;

    @Schema(description = "The configuration name to use for the merge operation", defaultValue = NamedSettings.DEFAULT_NAME)
    private String configName;

    @Schema(description = "The ID of the configuration cache bucket")
    private String configCacheBucketId;

    @Schema(description = "List of WorkItem pairs to be considered in the merge operation", implementation = WorkItemsPair.class)
    private List<WorkItemsPair> pairs;

    @Schema(description = "Indicates if merging referenced Work Item allowed")
    private Boolean allowReferencedWorkItemMerge;

    @JsonIgnore
    public boolean isAllowedReferencedWorkItemMerge() {
        return Boolean.TRUE.equals(allowReferencedWorkItemMerge);
    }

    public String getConfigName() {
        return configName != null ? configName : NamedSettings.DEFAULT_NAME;
    }
}
