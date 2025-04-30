package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import ch.sbb.polarion.extension.diff_tool.util.OSUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CpuLoadEntryTest {

    @Test
    void testNewInstanceInitializesWithCurrentCpuUsage() {
        double expectedCpuUsage = 0.75;

        try (MockedStatic<OSUtils> osUtilsMock = Mockito.mockStatic(OSUtils.class)) {
            osUtilsMock.when(OSUtils::getCpuUsage).thenReturn(expectedCpuUsage);
            CpuLoadEntry cpuLoadEntry = new CpuLoadEntry();

            assertEquals(expectedCpuUsage, cpuLoadEntry.getValue(), 0.001);
            assertNotNull(cpuLoadEntry.getTimestamp());
            assertNotNull(cpuLoadEntry.getRawTimestamp());
        }
    }

}
