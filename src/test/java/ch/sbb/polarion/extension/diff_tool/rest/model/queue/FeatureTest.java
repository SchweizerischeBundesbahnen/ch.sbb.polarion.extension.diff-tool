package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeatureTest {

    @Test
    public void testWorkerFeaturesExcludesCpuLoad() {
        List<Feature> workerFeatures = Feature.workerFeatures();

        assertFalse(workerFeatures.contains(Feature.CPU_LOAD), "Worker features should not contain CPU_LOAD");
        assertEquals(Feature.values().length - 1, workerFeatures.size(), "Worker features should contain all features except CPU_LOAD");

        for (Feature feature : Feature.values()) {
            if (feature != Feature.CPU_LOAD) {
                assertTrue(workerFeatures.contains(feature), "Worker features should contain " + feature);
            }
        }
    }

}
