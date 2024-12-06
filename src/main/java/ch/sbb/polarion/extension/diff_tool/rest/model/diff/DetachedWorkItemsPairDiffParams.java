package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Input data to compare detached WorkItems")
public class DetachedWorkItemsPairDiffParams extends WorkItemsPairDiffParams<ProjectWorkItem> {
}
