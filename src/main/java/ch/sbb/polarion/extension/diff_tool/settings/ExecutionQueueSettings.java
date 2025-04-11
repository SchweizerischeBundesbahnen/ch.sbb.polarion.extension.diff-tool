package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature.DIFF_DETACHED_WORKITEMS;
import static ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature.DIFF_DOCUMENT_WORKITEMS;

public class ExecutionQueueSettings extends GenericNamedSettings<ExecutionQueueModel> {
    public static final String FEATURE_NAME = "executionQueue";

    public ExecutionQueueSettings() {
        super(FEATURE_NAME);
    }

    public ExecutionQueueSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull ExecutionQueueModel defaultValues() {

        Map<Feature, Integer> workers = Feature.workerFeatures().stream().collect(Collectors.toMap(
                feature -> feature,
                feature -> DIFF_DOCUMENT_WORKITEMS.equals(feature) || DIFF_DETACHED_WORKITEMS.equals(feature) ? 2 : 1
        ));

        Map<Integer, Integer> threads = IntStream.rangeClosed(1, Feature.workerFeatures().size()).boxed().collect(Collectors.toMap(
                thread -> thread,
                thread -> 2
        ));

        return ExecutionQueueModel.builder()
                .workers(workers)
                .threads(threads)
                .build();
    }
}
