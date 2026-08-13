package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Static metadata the Execution Queue admin page needs in order to render, previously injected into the
 * page by JSP scriptlets and a hardcoded JavaScript map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata describing the execution queue configuration options")
public class QueueConfigurationMeta {

    @Schema(description = "Features that can be assigned to a worker, in declaration order")
    private List<FeatureInfo> features;

    @Schema(description = "The synthetic CPU load series, which is charted but not assignable to a worker")
    private FeatureInfo cpuLoad;

    @Schema(description = "Number of configurable workers")
    private int workerCount;

    @Schema(description = "Maximum thread count recommended for this machine, used as the upper bound of the thread inputs")
    private int maxRecommendedThreads;

    @Schema(description = "Maximum number of entries a worker queue holds before rejecting further submissions")
    private int queueCapacity;
}
