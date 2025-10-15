package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@Schema(description = "Input data to compare detached WorkItems")
public class DetachedWorkItemsPairDiffParams extends WorkItemsPairDiffParams<ProjectWorkItem> {
}
