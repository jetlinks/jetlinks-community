package org.jetlinks.community.network.manager.debug;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.EventBusSpanExporter;
import org.jetlinks.core.trace.ProtocolTracer;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceDebugSubscriptionProvider implements SubscriptionProvider {
    private final EventBus eventBus;

    private final DeviceRegistry registry;

    @Override
    public String id() {
        return "device-debug";
    }

    @Override
    public String name() {
        return "设备诊断";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/debug/device/*/trace"};
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String deviceId = TopicUtils
            .getPathVariables("/debug/device/{deviceId}/trace", request.getTopic())
            .get("deviceId");
        return startDebug(deviceId);
    }

    /**
     * @param deviceId deviceId
     * @see DeviceTracer
     * @see EventBusSpanExporter
     */
    Flux<TraceData> startDebug(String deviceId) {
        if (TraceHolder.isDisabled()) {
            return Flux
                .just(TraceData
                          .of(TraceDataType.log,
                              true,
                              "0",
                              "error",
                              "链路追踪功能已禁用,请联系管理员.",
                              System.currentTimeMillis(),
                              System.currentTimeMillis()));
        }
        //订阅设备跟踪信息
        return Flux
            .merge(this
                       .getTraceData(DeviceTracer.SpanName.operation(deviceId, "*"))
                       .flatMap(this::convertDeviceTrace),
                   registry
                       .getDevice(deviceId)
                       .flatMap(device -> device
                           .getProtocol()
                           .map(pro -> ProtocolTracer.SpanName.operation(pro.getId(), "*")))
                       .flatMapMany(this::getTraceData)
                       .flatMap(this::convertProtocolTrace)
            );
    }

    private Mono<TraceData> convertProtocolTrace(SpanDataInfo traceData) {
        String errorInfo = traceData
            .getEvent(SemanticAttributes.EXCEPTION_EVENT_NAME)
            .flatMap(event -> event.getAttribute(SemanticAttributes.EXCEPTION_STACKTRACE))
            .orElse(null);
        String operation = traceData.getName().substring(traceData.getName().lastIndexOf("/") + 1);
        //协议跟踪只展示错误信息,因为协议无法定位到具体的设备,如果现实全部跟踪信息可能会有很多信息
        if (StringUtils.hasText(errorInfo)) {
            return Mono.just(TraceData
                                 .of(TraceDataType.log,
                                     true,
                                     traceData.getTraceId(),
                                     operation,
                                     getDeviceTraceDetail(traceData),
                                     traceData.getStartWithNanos() / 1000 / 1000,
                                     traceData.getStartWithNanos() / 1000 / 1000
                                 ));
        }
        return Mono.empty();

    }

    private boolean hasError(SpanDataInfo data) {
        return data
            .getEvent(SemanticAttributes.EXCEPTION_EVENT_NAME)
            .isPresent();
    }

    private Object getDeviceTraceDetail(SpanDataInfo data) {

        String message = data
            .getAttribute(DeviceTracer.SpanKey.message)
            .orElse(null);

        String response = data
            .getAttribute(DeviceTracer.SpanKey.response)
            .orElse(null);

        if (StringUtils.hasText(message)) {
            if (StringUtils.hasText(response)) {
                return String.join("\n\n", response);
            }
            return message;
        }

        if (StringUtils.hasText(response)) {
            return response;
        }

        String errorInfo = data
            .getEvent(SemanticAttributes.EXCEPTION_EVENT_NAME)
            .flatMap(event -> event.getAttribute(SemanticAttributes.EXCEPTION_STACKTRACE))
            .orElse(null);

        if (StringUtils.hasText(errorInfo)) {
            return errorInfo;
        }

        return JSON.toJSONString(data.getAttributes(), SerializerFeature.PrettyFormat);

    }

    private Mono<TraceData> convertDeviceTrace(SpanDataInfo traceData) {
        String name = traceData.getName();
        String operation = name.substring(name.lastIndexOf("/") + 1);
        return Mono.just(TraceData
                             .of(TraceDataType.data,
                                 hasError(traceData),
                                 traceData.getTraceId(),
                                 operation,
                                 getDeviceTraceDetail(traceData),
                                 traceData.getStartWithNanos() / 1000 / 1000,
                                 traceData.getStartWithNanos() / 1000 / 1000
                             ));
    }

    private Flux<SpanDataInfo> getTraceData(String name) {
        //启用跟踪
        Disposable disposable = enableSpan(name);

        return eventBus
            .subscribe(Subscription
                           .builder()
                           .subscriberId("device_debug_tracer")
                           .topics("/trace/*" + name)
                           .broker()
                           .local()
                           .build(),
                       SpanDataInfo.class)
            //完成时关闭跟踪
            .doFinally(s -> disposable.dispose());
    }

    private Disposable enableSpan(String name) {
        Disposable.Composite disposable = Disposables.composite();

        String id = IDGenerator.UUID.generate();

        eventBus
            .publish("/_sys/_trace/opt", new TraceOpt(id, name, true))
            .subscribe();

        disposable.add(() -> eventBus
            .publish("/_sys/_trace/opt", new TraceOpt(id, name, false))
            .subscribe());

        return disposable;
    }

    @Subscribe(value = "/_sys/_trace/opt", features = {Subscription.Feature.broker, Subscription.Feature.local})
    public Mono<Void> handleTraceEnable(TraceOpt opt) {
        if (opt.enable) {
            log.debug("enable trace {} id:{}", opt.span, opt.id);
            TraceHolder.enable(opt.span, opt.id);
        } else {
            log.debug("remove trace {} id:{}", opt.span, opt.id);
            TraceHolder.removeEnabled(opt.span, opt.id);
        }
        return Mono.empty();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TraceOpt {
        private String id;
        private String span;
        private boolean enable;
    }

    public enum TraceDataType {
        /**
         * 和设备有关联的数据
         */
        data,
        /**
         * 和设备没有关联的日志信息
         */
        log
    }

    @Setter
    @Getter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    @ToString
    public static class TraceData implements Serializable {

        static Set<String> downstreamOperation = new HashSet<>(
            Arrays.asList(
                "downstream", "encode", "request"
            )
        );
        private static final long serialVersionUID = 1L;
        // 跟踪数据类型
        private TraceDataType type;
        //是否有错误信息
        private boolean error;
        // 跟踪ID
        private String traceId;
        /**
         * @see DeviceTracer.SpanName
         * 操作. encode,decode
         */
        private String operation;
        // 数据内容
        private Object detail;
        //开始时间 毫秒
        private long startTime;
        //结束时间 毫秒
        private long endTime;

        //是否上行操作
        public boolean isUpstream() {
            return !isDownstream();
        }

        //是否下行操作
        public boolean isDownstream() {
            return operation != null && downstreamOperation.contains(operation);
        }
    }
}
