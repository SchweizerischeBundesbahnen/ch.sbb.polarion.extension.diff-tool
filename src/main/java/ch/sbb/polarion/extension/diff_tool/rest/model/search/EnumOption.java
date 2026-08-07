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
@Schema(description = "An enumeration value of a WorkItem field, with the icon Polarion renders for it")
public class EnumOption {
    @Schema(description = "Option ID")
    private String id;

    @Schema(description = "Option name as displayed to the user")
    private String name;

    @Schema(description = "URL of the option icon, if defined")
    private String iconUrl;
}
