package ch.sbb.polarion.extension.diff_tool.rest;

import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.settings.ExecutionQueueModel;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueService;
import ch.sbb.polarion.extension.diff_tool.settings.ExecutionQueueSettings;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class, CurrentContextExtension.class})
class DiffToolRestApplicationTest {

    @Test
    void testConstructor() {
        ExecutionQueueModel model = new ExecutionQueueModel();
        Feature.workerFeatures().forEach(feature -> model.getWorkers().put(feature, 1));
        for (int i = 1; i <= ExecutionQueueService.WORKERS_COUNT; i++) {
            model.getThreads().put(i, 1);
        }

        try (MockedStatic<ExecutionQueueSettings> mockStaticSettings = mockStatic(ExecutionQueueSettings.class)) {
            mockStaticSettings.when(ExecutionQueueSettings::readAsSystemUser).thenReturn(model);

            DiffToolRestApplication app = new DiffToolRestApplication();
            assertEquals(10, app.getExtensionControllerClasses().size());
            assertNotNull(DiffToolRestApplication.getExecutionService());
            assertNotNull(DiffToolRestApplication.getExecutionMonitor());
        } finally {
            NamedSettingsRegistry.INSTANCE.getAll().clear();
        }
    }

}
