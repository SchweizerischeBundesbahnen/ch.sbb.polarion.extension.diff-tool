package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EndpointCallEntry extends TimeframeStatisticsEntry {

    private long queued;
    private long executing;

}
