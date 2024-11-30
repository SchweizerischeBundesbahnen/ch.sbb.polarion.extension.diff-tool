package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Represents a Polarion project with its ID and name")
public class Project {
    @Schema(description = "The project ID")
    private String id;

    @Schema(description = "The project name")
    private String name;

    @Schema(description = "Indicates whether the user is authorized for merge operations into this project")
    private boolean authorizedForMerge;
}
