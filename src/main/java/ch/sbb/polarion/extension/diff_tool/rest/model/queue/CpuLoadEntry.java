package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import ch.sbb.polarion.extension.diff_tool.util.OSUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CpuLoadEntry extends TimeframeStatisticsEntry {

    public static String ENTRY_ID = "CPU_LOAD";

    Double value = OSUtils.getCpuUsage();

}
