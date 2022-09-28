package org.jetlinks.community.dashboard.measurements.sys;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.measurements.MonitorObjectDefinition;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>系统监控支持</h1>
 * <p>
 * 支持获取cpu,内存,磁盘信息.支持实时监控
 * <p>
 * <h2>实时数据</h2>
 * 通过websocket(topic)或者sse(url)请求:
 * <p>
 * /dashboard/systemMonitor/stats/info/realTime
 *
 * <p>
 * <h3>参数:</h3>
 * <ul>
 * <li>type: memory,cpu,disk,其他则为全部信息</li>
 * <li>clusterNodeId: 指定获取集群节点的信息,不支持则返回所有节点的监控信息</li>
 * <li>agg: 在没有指定clusterNodeId时有效,设置为avg表示所有集群节点的平均值,sum为总和.</li>
 * </ul>
 *
 * <h3>响应结构:</h3>
 * <p>
 * 类型不同结构不同,memory: {@link MemoryInfo},cpu:{@link CpuInfo},disk:{@link DiskInfo},all:{@link SystemInfo}
 * <p>
 *  🌟: 企业版支持集群监控以及历史记录
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
public class SystemMonitorMeasurementProvider extends StaticMeasurementProvider {

    private final SystemMonitorService monitorService = new SystemMonitorServiceImpl();


    public SystemMonitorMeasurementProvider() {
        super(DefaultDashboardDefinition.systemMonitor, MonitorObjectDefinition.stats);

        addMeasurement(new StaticMeasurement(CommonMeasurementDefinition.info)
                           .addDimension(new RealTimeDimension())
        );


    }


    private void putTo(String prefix, MonitorInfo<?> source, Map<String, Object> target) {
        Map<String, Object> data = FastBeanCopier.copy(source, new HashMap<>());
        data.forEach((key, value) -> {
            char[] keyChars = key.toCharArray();
            keyChars[0] = Character.toUpperCase(keyChars[0]);
            target.put(prefix + new String(keyChars), value);
        });
    }


    //实时监控
    class RealTimeDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.realTime;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType();
        }

        @Override
        public ConfigMetadata getParams() {

            return new DefaultConfigMetadata()
                .add("serverNodeId", "服务节点ID", StringType.GLOBAL)
                .add("interval", "更新频率", StringType.GLOBAL)
                .add("type", "指标类型", new EnumType()
                    .addElement(EnumType.Element.of("all", "全部"))
                    .addElement(EnumType.Element.of("cpu", "CPU"))
                    .addElement(EnumType.Element.of("memory", "内存"))
                    .addElement(EnumType.Element.of("disk", "硬盘"))
                );
        }

        @Override
        public boolean isRealTime() {
            return true;
        }

        @Override
        @SuppressWarnings("all")
        public Publisher<? extends MeasurementValue> getValue(MeasurementParameter parameter) {
            Duration interval = parameter.getDuration("interval", Duration.ofSeconds(1));
            String type = parameter.getString("type", "all");

            return Flux
                .concat(
                    info(monitorService, type),
                    Flux
                        .interval(interval)
                        .flatMap(ignore -> info(monitorService, type))
                )
                .map(info -> SimpleMeasurementValue.of(info, System.currentTimeMillis()));
        }

        private Mono<? extends MonitorInfo<?>> info(SystemMonitorService service, String type) {
            Mono<? extends MonitorInfo<?>> data;
            switch (type) {
                case "cpu":
                    data = service.cpu();
                    break;
                case "memory":
                    data = service.memory();
                    break;
                case "disk":
                    data = service.disk();
                    break;
                default:
                    data = service.system();
                    break;
            }
            return data
                .onErrorResume(err -> Mono.empty());
        }


    }

}
