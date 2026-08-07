package ch.sbb.polarion.extension.diff_tool.rest.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One row of the WorkItem selection table")
public class SearchWorkItem {
    @Schema(description = "Unique identifier of the WorkItem")
    private String id;

    @Schema(description = "Project ID of the WorkItem")
    private String projectId;

    @Schema(description = "Title of the WorkItem")
    private String title;

    @Schema(description = "Type of the WorkItem")
    private EnumOption type;

    @Schema(description = "Status of the WorkItem")
    private EnumOption status;

    @Schema(description = "Severity of the WorkItem")
    private EnumOption severity;

    @Schema(description = "False when the item is unresolvable or the user may not read it")
    private boolean readable;

    @Schema(description = "Reason why the item is not readable, null otherwise")
    private String unavailableMessage;
}
