package org.jetlinks.community.dashboard.measurements;

import lombok.SneakyThrows;
import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static java.math.BigDecimal.ROUND_HALF_UP;

/**
 * 实时CPU 使用率监控
 * <pre>
 *     /dashboard/systemMonitor/cpu/usage/realTime
 * </pre>
 *
 * @author zhouhao
 */
@Component
public class SystemCpuMeasurementProvider
    extends StaticMeasurementProvider {

    public SystemCpuMeasurementProvider() {
        super(DefaultDashboardDefinition.systemMonitor, SystemObjectDefinition.cpu);
        addMeasurement(cpuUseAgeMeasurement);
    }

    static DataType type = new DoubleType().scale(1).min(0).max(100).unit(UnifyUnit.percent);

    static StaticMeasurement cpuUseAgeMeasurement = new StaticMeasurement(CommonMeasurementDefinition.usage)
        .addDimension(new CpuRealTimeMeasurementDimension());

    static OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

    static Callable<Double> processCpuUsage;
    static Callable<Double> systemCpuUsage;

    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList(
        "com.sun.management.OperatingSystemMXBean", // HotSpot
        "com.ibm.lang.management.OperatingSystemMXBean" // J9
    );

    static {
        Class<?> mxBeanClass = null;
        for (String s : OPERATING_SYSTEM_BEAN_CLASS_NAMES) {
            try {
                mxBeanClass = Class.forName(s);
            } catch (Exception ignore) {

            }
        }
        try {
            if (mxBeanClass != null) {
                Method method = mxBeanClass.getMethod("getProcessCpuLoad");
                Method system = mxBeanClass.getMethod("getSystemCpuLoad");
                processCpuUsage = () -> (double) method.invoke(osMxBean);
                systemCpuUsage = () -> (double) system.invoke(osMxBean);
            } else {
                processCpuUsage = () -> 0D;
                systemCpuUsage = () -> 0D;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class CpuRealTimeMeasurementDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return type;
        }

        @Override
        public ConfigMetadata getParams() {
            return null;
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @SneakyThrows
        public double getProcessCpu() {
            return processCpuUsage.call() * 100;
        }

        @SneakyThrows
        public double getSystemCpu() {
            return systemCpuUsage.call() * 100;
        }

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            //每秒获取系统CPU使用率
            return Flux.interval(Duration.ofSeconds(1))
                .map(t -> SimpleMeasurementValue.of(BigDecimal.valueOf(getSystemCpu()).setScale(1, ROUND_HALF_UP),
                    DateFormatter.toString(new Date(), "HH:mm:ss"),
                    System.currentTimeMillis()))
                .cast(MeasurementValue.class);
        }

    }

}
