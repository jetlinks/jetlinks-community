package org.jetlinks.community.device.debug.debug;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.EventBusSpanExporter;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.community.protocol.monitor.ProtocolMonitorHelper;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceTraceHelper {

    private final EventBus eventBus;

    private final ProtocolMonitorHelper monitorHelper;

    /**
     * @param deviceId deviceId
     * @see DeviceTracer
     * @see EventBusSpanExporter
     */
    public Flux<TraceData> startTracing(String deviceId) {
        if (TraceHolder.isDisabled()) {
            TraceData data = new TraceData();
            data.setType(TraceDataType.data);
            data.setOperation("error");
            data.setLogLevel("ERROR");
            data.setDetail("链路追踪功能已禁用,请联系管理员.");
            return Flux.just(data);
        }

        // todo 设备身份标识也作为设备ID进行订阅
        @SuppressWarnings("all")
        Flux<String> deviceIds = Flux
            .just(deviceId);

        return Flux
            .merge(
                // 链路追踪
                deviceIds
                    .flatMapIterable(_deviceId -> List
                        .of(
                            DeviceTracer.SpanName.operation0(_deviceId, "*"),
                            ProtocolMonitorHelper
                                .createDeviceTraceSpanPrefix("*", _deviceId)
                                .append("**")
                        ))
                    .collectList()
                    .flatMapMany(this::trace)
                    .map(TraceData::of),
                // 设备日志
                deviceIds
                    .flatMap(_deviceId -> monitorHelper.subscribeDeviceLog(_deviceId, "*"))
                    .map(TraceData::of)
            );

    }
    static final SharedPathString TRACE_PREFIX = SharedPathString.of("/trace/*");

    public Flux<SpanDataInfo> trace(Collection<? extends CharSequence> span) {
        CharSequence[] topics = span
            .stream()
            .map(TRACE_PREFIX::append)
            .toArray(CharSequence[]::new);
        // 启用链路追踪
        Disposable cancel = this.enable(span);
        if (log.isDebugEnabled()) {
            log.debug("subscribe trace span {}", Arrays.toString(topics));
        }
        return eventBus
            .subscribe(
                Subscription
                    .builder()
                    .subscriberId("dynamic-tracing")
                    .topics(topics)
                    .local()
                    .broker()
                    .build(),
                SpanDataInfo.class
            )
            // 订阅结束时取消之前的启用操作，避免不必要的浪费。
            .doFinally(ignore -> cancel.dispose());
    }


    public Disposable enable(Collection<? extends CharSequence> span) {
        String holder = IDGenerator.UUID.generate();
        for (CharSequence s : span) {
            TraceHolder.enable(s, holder);
        }
        return () -> {
            for (CharSequence s : span) {
                TraceHolder.removeEnabled(s, holder);
            }
        };
    }
}
