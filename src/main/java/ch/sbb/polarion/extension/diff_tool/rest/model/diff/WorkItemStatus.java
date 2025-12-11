package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WorkItem status data")
public class WorkItemStatus implements Comparable<WorkItemStatus> {
    @Schema(description = "Status ID")
    private String id;

    @Schema(description = "Status name")
    private String name;

    @Schema(description = "WorkItem type ID")
    private String wiTypeId;

    @Schema(description = "WorkItem type name")
    private String wiTypeName;

    @Override
    public int compareTo(@NotNull WorkItemStatus o) {
        return id.compareTo(o.id);
    }
}
