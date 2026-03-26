package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Merge input data")
public class MergeParams {
    @Schema(description = "The direction of the merge operation", implementation = MergeDirection.class)
    private MergeDirection mergeDirection;

    @Schema(description = "The role of the link connecting work item pairs")
    private String linkRole;

    @Schema(description = "The direction of the link role for newly created WorkItem", implementation = LinkRoleDirection.class)
    private LinkRoleDirection linkRoleDirection = LinkRoleDirection.DIRECT;

    @Schema(description = "The configuration name to use for the merge operation", defaultValue = NamedSettings.DEFAULT_NAME)
    private String configName = NamedSettings.DEFAULT_NAME;

    @Schema(description = "The ID of the configuration cache bucket")
    private String configCacheBucketId;

    @Schema(description = "Indicates whether attachment references should be updated in rich text fields when attachments are merged")
    private boolean updateAttachmentReferences;

    @Schema(description = "List of WorkItem pairs to be considered in the merge operation", implementation = MergeWorkItemsPair.class)
    private List<MergeWorkItemsPair> pairs;

    public LinkRoleDirection getLinkRoleDirection() {
        return linkRoleDirection != null ? linkRoleDirection : LinkRoleDirection.DIRECT;
    }

    public String getConfigName() {
        return configName != null ? configName : NamedSettings.DEFAULT_NAME;
    }
}
