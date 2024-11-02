package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a field in a diff operation")
public class DiffField {
    public static final DiffField TITLE = DiffField.builder().key("title").build();
    public static final DiffField OUTLINE_NUMBER = DiffField.builder().key("outlineNumber").build();
    public static final DiffField EXTERNAL_PROJECT_WORK_ITEM = DiffField.builder().key("externalProjectWorkItem").build();

    @Schema(description = "The key associated with the diff field")
    private String key;

    @Schema(description = "The type ID of the WorkItem related to the diff field")
    private String wiTypeId;
}
