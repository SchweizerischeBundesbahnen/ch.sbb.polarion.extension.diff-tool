package ch.sbb.polarion.extension.diff_tool.rest;

import ch.sbb.polarion.extension.diff_tool.rest.controller.ConversionApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.ConversionInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.DiffApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.DiffInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.MergeApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.MergeInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.ExecutionQueueQueueManagementApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.ExecutionQueueManagementInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.UtilityApiController;
import ch.sbb.polarion.extension.diff_tool.rest.controller.UtilityInternalController;
import ch.sbb.polarion.extension.diff_tool.rest.exception.QueueFullExceptionMapper;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueService;
import ch.sbb.polarion.extension.diff_tool.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DiffToolRestApplication extends GenericRestApplication {

    private static final AtomicReference<ExecutionQueueService> executionService = new AtomicReference<>();
    private static final AtomicReference<ExecutionQueueMonitor> executionMonitor = new AtomicReference<>();

    public DiffToolRestApplication() {
        ExecutionQueueSettings executionQueueSettings = new ExecutionQueueSettings();
        NamedSettingsRegistry.INSTANCE.register(
                Arrays.asList(
                        new DiffSettings(),
                        new AuthorizationSettings(),
                        executionQueueSettings
                )
        );
        initializeServices(executionQueueSettings);
    }

    private static void initializeServices(ExecutionQueueSettings executionQueueSettings) {
        ExecutionQueueService service = new ExecutionQueueService();
        ExecutionQueueMonitor monitor = new ExecutionQueueMonitor(service);
        executionQueueSettings.setSettingsChangedCallback(monitor::refreshConfiguration);
        executionService.set(service);
        executionMonitor.set(monitor);
    }

    public static ExecutionQueueService getExecutionService() {
        return executionService.get();
    }

    public static ExecutionQueueMonitor getExecutionMonitor() {
        return executionMonitor.get();
    }

    @Override
    @NotNull
    protected Set<Class<?>> getExtensionControllerClasses() {
        return Set.of(
                ConversionApiController.class,
                ConversionInternalController.class,
                DiffApiController.class,
                DiffInternalController.class,
                MergeApiController.class,
                MergeInternalController.class,
                UtilityInternalController.class,
                UtilityApiController.class,
                ExecutionQueueManagementInternalController.class,
                ExecutionQueueQueueManagementApiController.class
        );
    }

    @Override
    protected @NotNull Set<Object> getExtensionExceptionMapperSingletons() {
        return Set.of(new QueueFullExceptionMapper());
    }
}
