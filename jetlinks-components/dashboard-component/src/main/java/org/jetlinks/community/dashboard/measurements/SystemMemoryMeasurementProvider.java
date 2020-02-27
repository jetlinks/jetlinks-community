package org.jetlinks.community.dashboard.measurements;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;

import static java.math.BigDecimal.ROUND_HALF_UP;

/**
 * 实时内存使用率监控
 * <pre>
 *     /dashboard/systemMonitor/memory/info/realTime
 * </pre>
 *
 * @author zhouhao
 */
@Component
public class SystemMemoryMeasurementProvider extends StaticMeasurementProvider {
    public SystemMemoryMeasurementProvider() {
        super(DefaultDashboardDefinition.systemMonitor, MonitorObjectDefinition.memory);
        addMeasurement(systemMemoryInfo);
    }

    static ObjectType type = new ObjectType();

    static {
        {
            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId("max");
            metadata.setName("最大值");
            metadata.setValueType(new LongType());
            type.addPropertyMetadata(metadata);
        }

        {
            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId("used");
            metadata.setName("已使用");
            metadata.setValueType(new LongType());
            type.addPropertyMetadata(metadata);
        }

        {
            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId("usage");
            metadata.setName("使用率");
            metadata.setValueType(new DoubleType());
            type.addPropertyMetadata(metadata);
        }

    }

    static StaticMeasurement systemMemoryInfo = new StaticMeasurement(CommonMeasurementDefinition.info)
        .addDimension(new JvmMemoryInfoDimension());

    static class JvmMemoryInfoDimension implements MeasurementDimension {

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

        @Override
        public Flux<MeasurementValue> getValue(MeasurementParameter parameter) {
            return Flux.concat(
                Flux.just(MemoryInfo.of()),
                Flux.interval(Duration.ofSeconds(1))
                    .map(t -> MemoryInfo.of())
                    .windowUntilChanged(MemoryInfo::getUsage)
                    .flatMap(Flux::last))
                .map(val -> SimpleMeasurementValue.of(val,
                    DateFormatter.toString(new Date(), "HH:mm:ss"),
                    System.currentTimeMillis()))
                .cast(MeasurementValue.class);
        }

    }

    @Getter
    @Setter
    public static class MemoryInfo {
        private long max;

        private long used;

        private double usage;

        public static MemoryInfo of() {
            MemoryInfo info = new MemoryInfo();
            long total = (long) SystemMonitor.totalSystemMemory.getValue();

            info.max = total;
            info.used = (long) (total-  SystemMonitor.freeSystemMemory.getValue());
            info.usage = BigDecimal.valueOf(((double) info.getUsed() / info.getMax()) * 100D).setScale(2, ROUND_HALF_UP).doubleValue();
            return info;
        }
    }
}
