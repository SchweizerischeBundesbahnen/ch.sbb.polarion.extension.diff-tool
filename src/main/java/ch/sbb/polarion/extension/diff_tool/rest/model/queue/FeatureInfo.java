package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

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
@Schema(description = "A queueable feature, as the Execution Queue admin page needs to display it")
public class FeatureInfo {

    @Schema(description = "Feature ID, matching the keys used in the execution queue settings")
    private String id;

    @Schema(description = "Short label shown in the workers table")
    private String label;

    @Schema(description = "Longer explanation shown as additional info next to the label")
    private String description;

    public static @NotNull FeatureInfo of(@NotNull Feature feature) {
        return FeatureInfo.builder()
                .id(feature.name())
                .label(feature.getLabel())
                .description(feature.getDescription())
                .build();
    }
}
