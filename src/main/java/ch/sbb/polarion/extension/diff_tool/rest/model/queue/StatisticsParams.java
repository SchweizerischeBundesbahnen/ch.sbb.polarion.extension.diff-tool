package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Documents merge input data")
public class StatisticsParams {

    @Schema(description = "Starting timestamps for each worker")
    private Map<String, String> from = new HashMap<>();

    public List<TimeframeStatisticsEntry> filterQueue(String workerId, CircularFifoQueue<? extends TimeframeStatisticsEntry> queue) {
        List<TimeframeStatisticsEntry> list = new ArrayList<>(queue);
        String dateString = from.get(workerId);
        if (StringUtils.isEmpty(dateString)) {
            return list;
        } else {
            OffsetDateTime fromDate = OffsetDateTime.parse(dateString);
            return list.stream().filter(entry -> entry.getRawTimestamp().isAfter(fromDate)).toList();
        }
    }

}
