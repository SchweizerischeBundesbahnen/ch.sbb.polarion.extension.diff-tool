package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Schema(description = "Queued Execution Statistics")
public class Statistics {

    private Instant from;
    private Instant to;

    private Map<String, TimeframeStatisticsEntry> timeframeStatistics;

}
