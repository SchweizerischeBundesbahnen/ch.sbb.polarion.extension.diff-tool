package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Schema(description = "WorkItem field data")
public class WorkItemField implements Comparable<WorkItemField> {
    @Schema(description = "Unique key for the field")
    private String key;

    @Schema(description = "Name of the field")
    private String name;

    @Schema(description = "If field is required")
    private boolean required;

    @Schema(description = "If field is read only")
    private boolean readOnly;

    @Schema(description = "If field is custom")
    private boolean customField;

    @Schema(description = "Field's default value")
    @JsonIgnore
    private Object defaultValue;

    @Schema(description = "WorkItem type ID")
    private String wiTypeId;

    @Schema(description = "WorkItem type name")
    private String wiTypeName;

    @Override
    public int compareTo(@NotNull WorkItemField other) {
        return this.name.compareTo(other.name);
    }
}
