package ch.sbb.polarion.extension.diff_tool.util;

import com.sun.management.OperatingSystemMXBean;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OSUtilsTest {

    @Test
    void testReturnsValidCpuUsage() {
        OperatingSystemMXBean mockBean = mock(OperatingSystemMXBean.class);
        double expectedCpuLoad = 0.75;

        try (MockedStatic<ManagementFactory> mockedFactory = mockStatic(ManagementFactory.class)) {
            mockedFactory.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(mockBean);
            when(mockBean.getCpuLoad()).thenReturn(expectedCpuLoad);

            double result = OSUtils.getCpuUsage();
            assertEquals(expectedCpuLoad, result, 0.001);
        }
    }

    @Test
    void testReturnsAvailableProcessors() {
        int expectedProcessors = Runtime.getRuntime().availableProcessors();
        int actualProcessors = OSUtils.getMaxRecommendedParallelThreads();
        assertEquals(expectedProcessors, actualProcessors);
    }

}
