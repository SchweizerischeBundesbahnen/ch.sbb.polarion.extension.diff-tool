package ch.sbb.polarion.extension.diff_tool.widgets;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
public class WorkItemsDiffWidgetParams {
    private String targetProject;
    private String linkRole;
    private String configuration;
    private String query;
    private String page;
    private String recordsPerPage;
}
