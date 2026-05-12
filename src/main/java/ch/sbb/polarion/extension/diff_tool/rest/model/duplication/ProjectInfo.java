package ch.sbb.polarion.extension.diff_tool.rest.model.duplication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lightweight view of a Polarion project for selection lists")
public class ProjectInfo {
    @Schema(description = "Project id")
    private String id;

    @Schema(description = "Project display name")
    private String name;
}
