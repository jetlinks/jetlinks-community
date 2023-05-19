package org.jetlinks.community.dashboard.measurements.sys;

import com.google.common.collect.Maps;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.measurements.MonitorObjectDefinition;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Date;
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
 *
 * <h2>历史数据</h2>
 *
 * <pre>{@code
 * POST /dashboard/_multi
 *
 *  [
 *     {
 *         "dashboard": "systemMonitor",
 *         "object": "stats",
 *         "measurement": "info",
 *         "dimension": "history",
 *         "group": "system-monitor",
 *         "params": {
 *              "from":"now-10m",
 *              "to":"now"
 *         }
 *     }
 * ]
 *
 * 返回:
 *
 *  [
 *    {
 *    "group":"system-monitor",
 *    "data": {
 *          "value": {
 *              "memorySystemFree": 344, //系统可用内存
 *              "memoryJvmHeapFree": 3038, //jvm可用内存
 *              "memorySystemTotal": 49152, //系统总内存
 *              "memoryJvmNonHeapTotal": 49152, //jvm堆外总内存
 *              "diskTotal": 1907529, //磁盘总空间
 *              "cpuSystemUsage": 11.8, //系统cpu使用率
 *              "diskFree": 1621550, //磁盘可用空间
 *              "clusterNodeId": "jetlinks-platform:8820", //集群节点ID
 *              "memoryJvmHeapTotal": 4001, //jvm总内存
 *              "cpuJvmUsage": 0.1, //jvm cpu使用率
 *              "memoryJvmNonHeapFree": 48964, //jvm堆外可用内存
 *              "id": "eSEeBYEBN57nz4ZBo0WI", // ID
 *          },
 *          "timeString": "2023-05-16 18:32:27",//时间
 *          "timestamp": 1684233147193 //时间
 *       }
 *    }
 *  ]
 *
 * }</pre>
 *
 *  🌟: 企业版支持集群监控
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
public class SystemMonitorMeasurementProvider extends StaticMeasurementProvider {

    private final SystemMonitorService monitorService = new SystemMonitorServiceImpl();

    private final Duration collectInterval = TimeUtils.parse(System.getProperty("monitor.system.collector.interval", "1m"));

    private final Scheduler scheduler;

    private final TimeSeriesManager timeSeriesManager;

    static final TimeSeriesMetric metric = TimeSeriesMetric.of(System.getProperty("monitor.system.collector.metric", "system_monitor"));

    private final Disposable.Composite disposable = Disposables.composite();


    public SystemMonitorMeasurementProvider(TimeSeriesManager timeSeriesManager) {
        super(DefaultDashboardDefinition.systemMonitor, MonitorObjectDefinition.stats);
        this.timeSeriesManager = timeSeriesManager;

        addMeasurement(new StaticMeasurement(CommonMeasurementDefinition.info)
            .addDimension(new RealTimeDimension())
            .addDimension(new HistoryDimension())
        );

        this.scheduler = Schedulers.newSingle("system-monitor-collector");

        disposable.add(this.scheduler);
    }

    @PreDestroy
    public void destroy() {
        disposable.dispose();
    }

    @PostConstruct
    public void init() {
        //注册监控信息
        timeSeriesManager
            .registerMetadata(
                TimeSeriesMetadata.of(metric)
            )
            .block(Duration.ofSeconds(10));

        //定时收集监控信息
        disposable.add(Flux
            .interval(collectInterval, scheduler)
            .flatMap(ignore -> monitorService
                .system()
                .map(this::systemInfoToMap)
                .flatMap(data -> timeSeriesManager.getService(metric).commit(data))
                .onErrorResume(err -> Mono.empty()))
            .subscribe()
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

    public TimeSeriesData systemInfoToMap(SystemInfo info) {
        Map<String, Object> map = Maps.newLinkedHashMapWithExpectedSize(12);
        putTo("cpu", info.getCpu(), map);
        putTo("disk", info.getDisk(), map);
        putTo("memory", info.getMemory(), map);
        return TimeSeriesData.of(System.currentTimeMillis(), map);
    }

    //历史记录
    class HistoryDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.history;
        }

        @Override
        public DataType getValueType() {
            return new ObjectType();
        }

        @Override
        public ConfigMetadata getParams() {
            return new DefaultConfigMetadata();
        }

        @Override
        public boolean isRealTime() {
            return false;
        }

        @Override
        public Flux<? extends MeasurementValue> getValue(MeasurementParameter parameter) {
            Date from = parameter.getDate("from", TimeUtils.parseDate("now-1h"));
            Date to = parameter.getDate("to", TimeUtils.parseDate("now"));

            return QueryParamEntity
                .newQuery()
                .noPaging()
                .between("timestamp", from, to)
                .execute(timeSeriesManager.getService(metric)::query)
                .map(tsData -> SimpleMeasurementValue.of(tsData.getData(), tsData.getTimestamp()));
        }
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
