package org.jetlinks.community.device.debug.debug;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetlinks.community.log.LogRecord;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.data.SpanDataInfo;
import org.jetlinks.core.trace.data.SpanEventDataInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class TraceData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    static Set<String> downstreamOperation = Set
        .of(
            DeviceTracer.OperationName.downstream,
            DeviceTracer.OperationName.encode,
            DeviceTracer.OperationName.request
        );

    // 跟踪数据类型
    private TraceDataType type;
    //是否有错误信息
    private boolean error;

    private String message;

    // 跟踪ID
    private String traceId;

    private String spanId;

    private String parentSpanId;

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

    //开始时间 毫秒
    private long startTimeNano;
    //结束时间 毫秒
    private long endTimeNano;

    /**
     * 日志级别（仅 type=log 时有效），如 TRACE、DEBUG、INFO、WARN、ERROR
     */
    private String logLevel;

    public TraceData() {
    }

    public static TraceData of(SpanDataInfo span) {
        TraceData data = new TraceData();
        String name = span.getName();
        String operation = name.substring(name.lastIndexOf("/") + 1);

        SpanEventDataInfo exception = span.getEvent("exception").orElse(null);
        if (exception != null) {
            data.setError(true);
            exception
                .getAttribute("exception.message")
                .map(String::valueOf)
                .ifPresent(data::setMessage);
        }
        data.setType(TraceDataType.data);
        data.setOperation(operation);
        data.setStartTime(span.getStartWithNanos() / 1000 / 1000);
        data.setEndTime(span.getEndWithNanos() / 1000 / 1000);
        data.setStartTimeNano(span.getStartWithNanos());
        data.setEndTimeNano(span.getEndWithNanos());
        data.setTraceId(span.getTraceId());
        data.setSpanId(span.getSpanId());
        data.setParentSpanId(span.getParentSpanId());
        data.setDetail(getDeviceTraceDetail(span));

        return data;
    }

    public static TraceData of(LogRecord log) {

        TraceData data = new TraceData();
        data.setType(TraceDataType.log);
        data.setOperation("log");
        data.setStartTime(log.getTimestamp());
        data.setEndTime(log.getTimestamp());
        data.setMessage(log.getMessage());
        data.setDetail(log.getErrorDetail() != null ? log.getErrorDetail() : log.getMessage());
        data.setLogLevel(log.getLevel().name());
        data.setTraceId(log.getTraceId());
        data.setSpanId(log.getSpanId());

        return data;
    }


    //是否上行操作
    public boolean isUpstream() {
        return !isDownstream();
    }

    //是否下行操作
    public boolean isDownstream() {
        return operation != null && downstreamOperation.contains(operation);
    }


    static boolean hasError(SpanDataInfo data) {
        return data
            .getEvent("exception")
            .isPresent();
    }

    private static Object getDeviceTraceDetail(SpanDataInfo data) {

        return Map.of(
            // 属性,不同的链路有不同的属性.
            "attrs", data.getAttributes() == null ? Map.of() : Maps.filterKeys(data.getAttributes(), k -> !k.equals("flux-next-count")),
            // 时间,错误等信息.
            "events", data.getEvents() == null ? List.of() : data.getEvents()
        );

    }
}