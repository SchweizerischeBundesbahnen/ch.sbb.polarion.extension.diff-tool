package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.DiffToolRestApplication;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.Feature;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.FeatureInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.queue.QueueConfigurationMeta;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionQueueMonitor;
import ch.sbb.polarion.extension.diff_tool.service.queue.ExecutionWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Covers /queue/configuration-meta, which replaces the JSP scriptlets and the hardcoded
 * featuresLocalization map the Execution Queue page used to be built from.
 */
class ExecutionQueueManagementInternalControllerTest {

    private MockedStatic<DiffToolRestApplication> mockedApp;
    private ExecutionQueueManagementInternalController controller;

    @BeforeEach
    void setUp() {
        mockedApp = mockStatic(DiffToolRestApplication.class);
        mockedApp.when(DiffToolRestApplication::getExecutionMonitor).thenReturn(mock(ExecutionQueueMonitor.class));
        controller = new ExecutionQueueManagementInternalController();
    }

    @AfterEach
    void tearDown() {
        mockedApp.close();
    }

    @Test
    void exposesEveryAssignableFeatureInDeclarationOrder() {
        QueueConfigurationMeta meta = controller.getConfigurationMeta();

        assertEquals(
                Feature.workerFeatures().stream().map(Feature::name).toList(),
                meta.getFeatures().stream().map(FeatureInfo::getId).toList());
    }

    @Test
    void excludesCpuLoadFromTheAssignableFeaturesAndReportsItSeparately() {
        QueueConfigurationMeta meta = controller.getConfigurationMeta();

        assertFalse(meta.getFeatures().stream().anyMatch(feature -> Feature.CPU_LOAD.name().equals(feature.getId())));
        assertEquals(Feature.CPU_LOAD.name(), meta.getCpuLoad().getId());
        assertEquals("CPU Load", meta.getCpuLoad().getLabel());
    }

    @Test
    void carriesALabelAndDescriptionForEveryFeature() {
        // The point of moving these out of JavaScript: a new enum constant can no longer leave the admin
        // page rendering a raw name and failing on a missing tooltip entry.
        QueueConfigurationMeta meta = controller.getConfigurationMeta();

        List<FeatureInfo> all = new java.util.ArrayList<>(meta.getFeatures());
        all.add(meta.getCpuLoad());
        for (FeatureInfo feature : all) {
            assertNotNull(feature.getLabel(), feature.getId() + " has no label");
            assertFalse(feature.getLabel().isBlank(), feature.getId() + " has a blank label");
            assertNotNull(feature.getDescription(), feature.getId() + " has no description");
            assertFalse(feature.getDescription().isBlank(), feature.getId() + " has a blank description");
        }
    }

    @Test
    void allocatesOneWorkerPerAssignableFeature() {
        QueueConfigurationMeta meta = controller.getConfigurationMeta();

        assertEquals(Feature.workerFeatures().size(), meta.getWorkerCount());
    }

    @Test
    void reportsTheRealThreadLimitAndQueueCapacity() {
        QueueConfigurationMeta meta = controller.getConfigurationMeta();

        assertEquals(Runtime.getRuntime().availableProcessors(), meta.getMaxRecommendedThreads());
        // Read from the constant rather than repeated as a literal, which is how the JSP's help text
        // drifted from the actual capacity.
        assertEquals(ExecutionWorker.DEFAULT_MAX_QUEUE_CAPACITY, meta.getQueueCapacity());
        assertTrue(meta.getQueueCapacity() > 0);
    }
}
