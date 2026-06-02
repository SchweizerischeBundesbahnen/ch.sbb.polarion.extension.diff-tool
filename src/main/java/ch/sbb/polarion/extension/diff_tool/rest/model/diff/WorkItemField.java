package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

@Data
@EqualsAndHashCode(of = {"name", "wiTypeName", "key"}) // equals/hashCode must stay aligned with compareTo, which orders by name, wiTypeName and key only
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
        int fieldNameDiff = this.name.compareTo(other.name);
        if (fieldNameDiff != 0) {
            return fieldNameDiff;
        }

        int wiTypeNameDiff = compareNullable(this.wiTypeName, other.wiTypeName);
        if (wiTypeNameDiff != 0) {
            return wiTypeNameDiff;
        }

        // Two fields can share the same name and work item type but still be distinct custom fields with different
        // keys, so the unique key is used as the final criterion to keep them apart (it stays 0 for the same field
        // defined both globally and on a work item type, which is what we want to deduplicate).
        return compareNullable(this.key, other.key);
    }

    @VisibleForTesting
    static int compareNullable(String first, String second) {
        if (first != null && second != null) {
            return first.compareTo(second);
        } else if (first != null) {
            return 1;
        } else if (second != null) {
            return -1;
        }
        return 0;
    }
}
