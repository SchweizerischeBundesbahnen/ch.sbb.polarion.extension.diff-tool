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
@Schema(description = "Parameters to duplicate a Polarion project via template export/import")
public class DuplicationRequest {
    @Schema(description = "ID of the existing project to duplicate", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceProjectId;

    @Schema(description = "ID for the new project being created", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetProjectId;

    @Schema(description = "Repository location path for the new project (e.g. /MyProjects/copy-of-foo)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Schema(description = "Tracker prefix for work items in the new project (e.g. ABC- ; the trailing dash is added automatically if missing)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackerPrefix;
}
