package ch.sbb.polarion.extension.diff_tool.rest.model.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The roles that exist and can therefore be picked on the Merge Authorization page. Not to be confused
 * with {@link AuthorizationModel}, which stores the roles actually granted permission to merge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Roles available for selection when authorizing merge operations")
public class RolesModel {

    @Schema(description = "All global roles defined in this Polarion instance")
    private List<String> globalRoles;

    @Schema(description = "Roles defined for the project of the requested scope; empty for a global scope")
    private List<String> projectRoles;
}
