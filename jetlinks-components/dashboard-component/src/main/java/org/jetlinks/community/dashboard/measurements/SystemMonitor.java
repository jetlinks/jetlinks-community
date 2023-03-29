package org.jetlinks.community.dashboard.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

@AllArgsConstructor
@Getter
public enum SystemMonitor {
    systemCpuUsage("系统CPU使用率"),
    jvmCpuUsage("JVM进程CPU使用率"),
    freeSystemMemory("系统空闲内存"),
    totalSystemMemory("系统总内存"),
    openFileCount("已打开文件数"),
    maxOpenFileCount("最大打开文件数"),
    ;

    private final String text;

    public double getValue() {
        return getValue(name());
    }

    static final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

    private final static Map<String, Callable<Double>> items = new HashMap<>();

    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
        "com.sun.management.OperatingSystemMXBean", // HotSpot
        "com.ibm.lang.management.OperatingSystemMXBean" // J9
    );

    private static final Callable<Double> zero = () -> 0D;

    private static Class<?> mxBeanClass;

    private static void register(String item, String methodName, Function<Double, Double> mapping) {
        try {
            Method method = mxBeanClass.getMethod(methodName);
            items.put(item, () -> mapping.apply(((Number) method.invoke(osMxBean)).doubleValue()));
        } catch (Exception ignore) {

        }
    }

    static {
        for (String s : OPERATING_SYSTEM_BEAN_CLASS_NAMES) {
            try {
                mxBeanClass = Class.forName(s);
            } catch (Exception ignore) {
            }
        }
        GlobalMemory memory = new oshi.SystemInfo()
            .getHardware()
            .getMemory();
        {
            items.put(freeSystemMemory.name(),()->memory.getAvailable()/ 1024 / 1024D);
            items.put(totalSystemMemory.name(),()->memory.getTotal()/ 1024 / 1024D);
        }
        try {
            if (mxBeanClass != null) {
                register(systemCpuUsage.name(), "getSystemCpuLoad", usage -> usage * 100D);
                register(jvmCpuUsage.name(), "getProcessCpuLoad", usage -> usage * 100D);
                register("virtualMemory", "getCommittedVirtualMemorySize", val -> val / 1024 / 1024);
                register(openFileCount.name(), "getOpenFileDescriptorCount", Function.identity());
                register(maxOpenFileCount.name(), "getMaxFileDescriptorCount", Function.identity());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double value() {
        return SystemMonitor.getValue(this.name());
    }

    @SneakyThrows
    public static double getValue(String id) {
        double val = items.getOrDefault(id, zero).call();

        return Double.isNaN(val) ? 0 : val;
    }

}
