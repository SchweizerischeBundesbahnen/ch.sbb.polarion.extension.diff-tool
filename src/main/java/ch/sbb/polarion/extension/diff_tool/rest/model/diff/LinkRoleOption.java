package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One selectable link role of a project, with both directions of its name.
 * <p>
 * Unlike {@link ch.sbb.polarion.extension.diff_tool.rest.model.settings.LinkRole}, which lists a role once per
 * WorkItem type for the Diff Configurations page, this is the plain per-project list the pickers offer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A link role of a project, as offered for selection")
public class LinkRoleOption {
    @Schema(description = "Role ID")
    private String id;

    @Schema(description = "Role name in the forward direction")
    private String name;

    @Schema(description = "Role name in the backward direction")
    private String oppositeName;
}
