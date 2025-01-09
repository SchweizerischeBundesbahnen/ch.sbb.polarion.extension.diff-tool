package ch.sbb.polarion.extension.diff_tool.widgets;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(fluent = true)
public class DiffWidgetParams {
    private String linkRole;
    private String configuration;
    private DataSetWidgetParams sourceParams;
    private DataSetWidgetParams targetParams;
}
