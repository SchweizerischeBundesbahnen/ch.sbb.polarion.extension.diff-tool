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
@Schema(description = "Represents a Polarion project with its ID, name and information about merge authorization")
public class Project {
    @Schema(description = "The project ID")
    private String id;

    @Schema(description = "The project name")
    private String name;

    @Schema(description = "Indicates whether the user is authorized for merge operations into this project")
    private boolean authorizedForMerge;
}
