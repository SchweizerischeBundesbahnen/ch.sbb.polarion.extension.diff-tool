package ch.sbb.polarion.extension.diff_tool.util;

import com.sun.management.OperatingSystemMXBean;
import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;

@UtilityClass
public class OSUtils {

    public double getCpuUsage() {
        // sometimes getCpuLoad returns -1
        return Math.max(0, ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getCpuLoad());
    }

}
