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

public class DiffToolRestApplication extends GenericRestApplication {

    private final ExecutionQueueService executionService;
    private final ExecutionQueueMonitor executionMonitor;

    public DiffToolRestApplication() {
        ExecutionQueueSettings executionQueueSettings = new ExecutionQueueSettings();
        NamedSettingsRegistry.INSTANCE.register(
                Arrays.asList(
                        new DiffSettings(),
                        new AuthorizationSettings(),
                        executionQueueSettings
                )
        );
        this.executionService = new ExecutionQueueService();
        this.executionMonitor = new ExecutionQueueMonitor(executionService);
        executionQueueSettings.setSettingsChangedCallback(executionMonitor::refreshConfiguration);

    }

    @Override
    @NotNull
    public Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new ConversionApiController(),
                new ConversionInternalController(),
                new DiffApiController(executionService),
                new DiffInternalController(executionService),
                new MergeApiController(),
                new MergeInternalController(),
                new UtilityInternalController(),
                new UtilityApiController(),
                new ExecutionQueueManagementInternalController(executionMonitor),
                new ExecutionQueueQueueManagementApiController(executionMonitor)
        );
    }

    @Override
    protected @NotNull Set<Object> getExtensionExceptionMapperSingletons() {
        return Set.of(new QueueFullExceptionMapper());
    }
}
