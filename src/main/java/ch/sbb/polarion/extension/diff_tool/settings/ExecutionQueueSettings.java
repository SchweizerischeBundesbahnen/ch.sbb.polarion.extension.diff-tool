package ch.sbb.polarion.extension.diff_tool.settings;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.util.OSUtils;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Setter
@SuppressWarnings("java:S2160") //Override the "equals" not needed here by design
public class ExecutionQueueSettings extends GenericNamedSettings<ExecutionQueueModel> {
    public static final String FEATURE_NAME = "executionQueue";
    private Runnable settingsChangedCallback;

    public ExecutionQueueSettings() {
        super(FEATURE_NAME);
    }

    public ExecutionQueueSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public void beforeSave(@NotNull ExecutionQueueModel model) {
        validateThreadsCount(model);
    }

    @Override
    public void afterSave(@NotNull ExecutionQueueModel what) {
        settingsChangedCallback.run();
    }

    @VisibleForTesting
    public void validateThreadsCount(@NotNull ExecutionQueueModel model) {
        if (model.getThreads().values().stream().anyMatch(t -> t < 1 || t > OSUtils.getMaxRecommendedParallelThreads())) {
            throw new IllegalArgumentException("Threads count must be between 1 and " + OSUtils.getMaxRecommendedParallelThreads());
        }
    }

    @Override
    public @NotNull ExecutionQueueModel defaultValues() {

        Map<Feature, Integer> workers = Feature.workerFeatures().stream().collect(Collectors.toMap(
                feature -> feature,
                feature -> 0
        ));

        Map<Integer, Integer> threads = IntStream.rangeClosed(1, Feature.workerFeatures().size()).boxed().collect(Collectors.toMap(
                thread -> thread,
                thread -> 1
        ));

        return ExecutionQueueModel.builder()
                .workers(workers)
                .threads(threads)
                .build();
    }

    public static ExecutionQueueModel readAsSystemUser() {
        return PlatformContext.getPlatform().lookupService(ISecurityService.class).doAsSystemUser(
                (PrivilegedAction<ExecutionQueueModel>) () -> (ExecutionQueueModel) NamedSettingsRegistry.INSTANCE.getByFeatureName(FEATURE_NAME).read(
                        "", SettingId.fromName(NamedSettings.DEFAULT_NAME), null));
    }
}
