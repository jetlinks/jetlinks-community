package org.jetlinks.community.network.manager.session;

import com.google.common.collect.Maps;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.dashboard.*;
import org.jetlinks.community.dashboard.supports.StaticMeasurement;
import org.jetlinks.community.dashboard.supports.StaticMeasurementProvider;
import org.jetlinks.community.gateway.monitor.measurements.GatewayObjectDefinition;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.community.utils.TimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import java.util.Map;

import static org.jetlinks.community.timeseries.query.Aggregation.DISTINCT_COUNT;

/**
 * 设备会话监控统计支持
 * <p>
 * 获取设备在线量统计(通过统计设备在线时长监控)
 *
 * <pre>{@code
 * POST /dashboard/_multi
 * [
 *     {
 *         "dashboard": "device",
 *         "object": "session",
 *         "measurement": "online",
 *         "dimension": "agg",
 *         "group": "sameDay",
 *         "params": {
 *              "format":"yyyy-MM-dd",
 *              "interval":"1d",
 *              "from":"now-1d",
 *              "to":"now",
 *              "type":"bytesSent",
 *              "limit":10
 *         }
 *     }
 * ]
 *
 * [
 * {
 * 	"group": "sameDay",
 * 	"data": {
 * 		"value": 3003,
 * 		"timeString": "2022-05-30",
 * 		"timestamp": 1653840000000
 *     }
 * }
 * ]
 *
 *
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
public class DeviceSessionMeasurementProvider extends StaticMeasurementProvider {

    private final DeviceSessionManager sessionManager;

    private final TimeSeriesMetric metric;

    private final TimeSeriesManager timeSeriesManager;

    //定时记录会话信息等间隔,此配置直接影响设备在线数量可统计的周期,默认为1小时. 值越小,数据量越大.
    private final Duration interval = TimeUtils.parse(System.getProperty("device.session.report.interval", "1h"));

    private final Disposable.Composite disposable = Disposables.composite();

    public DeviceSessionMeasurementProvider(DeviceSessionManager sessionManager,
                                            TimeSeriesManager timeSeriesManager) {
        super(DefaultDashboardDefinition.device, GatewayObjectDefinition.session);

        this.sessionManager = sessionManager;
        this.metric = TimeSeriesMetric.of("device_session_metric");
        this.timeSeriesManager = timeSeriesManager;

        addMeasurement(new StaticMeasurement(MeasurementDefinition.of("online", "在线统计"))
                           .addDimension(new DeviceSessionMeasurementDimension())
        );
    }


    class DeviceSessionMeasurementDimension implements MeasurementDimension {

        @Override
        public DimensionDefinition getDefinition() {
            return CommonDimensionDefinition.agg;
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

        private AggregationQueryParam createQuery(MeasurementParameter parameter) {
            return AggregationQueryParam
                .of()
                .agg("deviceId", "count", DISTINCT_COUNT)
                .groupBy(parameter.getInterval("interval", parameter.getInterval("time", null)),
                         parameter.getString("format", "yyyy-MM-dd"))
                .from(parameter.getDate("from", TimeUtils.parseDate("now-1d")))
                .to(parameter.getDate("to").orElseGet(Date::new))
                .filter(q -> q
                    .is("name", "duration")
                    //产品ID
                    .is("productId", parameter.getString("productId", null))
                    //设备ID
                    .is("deviceId", parameter.getString("deviceId", null))
                );

        }

        @Override
        public Publisher<? extends Object> getValue(MeasurementParameter parameter) {
            AggregationQueryParam queryParam = createQuery(parameter);
            String format = parameter.getString("format", "yyyy-MM-dd");
            DateTimeFormatter formatter = DateTimeFormat.forPattern(format);

            return timeSeriesManager
                .getService(metric)
                .aggregation(queryParam)
                .map(data -> SimpleMeasurementValue.of(
                    data.getLong("count", 0),
                    data.getString("time", ""),
                    data.getString("time").map(formatter::parseMillis).orElse(0L)
                ))
                .sort()
                .take(parameter.getInt("limit", 30));
        }
    }

    @PostConstruct
    public void init() {
        timeSeriesManager
            .registerMetadata(
                TimeSeriesMetadata.of(
                    metric,
                    SimplePropertyMetadata.of("type", "type", StringType.GLOBAL),
                    SimplePropertyMetadata.of("creatorId", "creatorId", StringType.GLOBAL),
                    SimplePropertyMetadata.of("deviceId", "deviceId", StringType.GLOBAL),
                    SimplePropertyMetadata.of("productId", "productId", StringType.GLOBAL),
                    SimplePropertyMetadata.of("server", "server", StringType.GLOBAL),
                    SimplePropertyMetadata.of("name", "name", StringType.GLOBAL),
                    SimplePropertyMetadata.of("duration", "duration", LongType.GLOBAL),
                    SimplePropertyMetadata.of("count", "count", LongType.GLOBAL),
                    SimplePropertyMetadata.of("connectTime", "connectTime", DateTimeType.GLOBAL)
                ))
            .block(Duration.ofSeconds(30));

        Scheduler scheduler = Schedulers.newSingle("device-session-reporter");

        disposable.add(scheduler);

        if (!interval.isZero() && !interval.isNegative()) {
            disposable.add(
                Flux
                    .interval(interval, scheduler)
                    .flatMap(ignore -> reportDeviceSession())
                    .subscribe()
            );
        }

        disposable.add(
            sessionManager
                .listenEvent(event -> reportDeviceSession(event.getSession(), "session"))
        );

    }

    @PreDestroy
    public void shutdown() {
        disposable.dispose();
    }

    private Mono<Void> reportDeviceSession() {
        return sessionManager
            .getSessions()
            .flatMap(this::reportDeviceSession)
            .onErrorResume(err -> Mono.empty())
            .then();
    }

    protected Mono<Void> reportDeviceSession(DeviceSession session) {
        return reportDeviceSession(session, "check");
    }

    private long computeDuration(long timestamp) {
        return System.currentTimeMillis() - timestamp;
    }

    protected Mono<Void> reportDeviceSession(DeviceSession session, String type) {
        if (null == session.getOperator()) {
            return Mono.empty();
        }
        return session
            .getOperator()
            .getSelfConfigs(DeviceConfigKey.productId)
            .map(configs -> {
                Map<String, Object> data = Maps.newHashMapWithExpectedSize(16);
                //以连接时间等构造id减少重复数据
                data.put("id", DigestUtils.md5Hex(
                    String.join(
                        "-",
                        session.getDeviceId(),
                        String.valueOf(session.connectTime()),
                        sessionManager.getCurrentServerId(),
                        //如果是会话注册注销,则更新原始会话记录
                        "session".equals(type) ? "session" : String.valueOf((int) (System.currentTimeMillis() / this.interval.toMillis()))
                    )
                ));
                data.put("name", "duration");
                data.put("productId", configs.getValue(DeviceConfigKey.productId).orElse(""));
                data.put("deviceId", session.getDeviceId());
                //所在集群节点ID
                data.put("server", sessionManager.getCurrentServerId());
                //上线时间
                data.put("connectTime", session.connectTime());
                //在线时长
                data.put("duration", computeDuration(session.connectTime()));
                data.put("type", type);
                return TimeSeriesData.of(System.currentTimeMillis(), data);
            })
            .as(
                timeSeriesManager
                    .getService(metric)
                    ::commit
            )
            .onErrorResume(err -> Mono.empty());
    }

}
