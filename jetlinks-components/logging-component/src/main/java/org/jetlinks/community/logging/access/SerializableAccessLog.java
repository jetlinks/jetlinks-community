package org.jetlinks.community.logging.access;

import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.logging.AccessLogger;
import org.hswebframework.web.logging.AccessLoggerInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @see org.hswebframework.web.logging.AccessLoggerInfo
 */
@Setter
@Getter
public class SerializableAccessLog implements Serializable {

    /**
     * 日志id
     */
    @Schema(description = "日志ID")
    private String id;

    /**
     * 访问的操作
     *
     * @see AccessLogger#value()
     */
    @Schema(description = "操作")
    private String action;

    /**
     * 描述
     *
     * @see AccessLogger#describe()
     */
    @Schema(description = "描述")
    private String describe;

    /**
     * 访问对应的java方法
     */
    @Schema(description = "请求方法名")
    private String method;

    /**
     * 访问对应的java类
     */
    @Schema(description = "请求类")
    private String target;

    /**
     * 请求的参数,参数为java方法的参数而不是http参数,key为参数名,value为参数值.
     */
    @Schema(description = "请求参数")
    private Map<String, Object> parameters;

    /**
     * 请求者ip地址
     */
    @Schema(description = "请求中IP")
    private String ip;

    /**
     * 请求的url地址
     */
    @Schema(description = "请求地址")
    private String url;

    /**
     * http 请求头集合
     */
    @Schema(description = "请求头")
    private Map<String, String> httpHeaders = new HashMap<>();

    @Schema(description = "上下文")
    private Map<String, String> context = new HashMap<>();

    /**
     * http 请求方法, GET,POST...
     */
    @Schema(description = "请求方法")
    private String httpMethod;

    /**
     * 响应结果,方法的返回值
     */
    //private Object response;

    /**
     * 请求时间戳
     *
     * @see System#currentTimeMillis()
     */
    @Schema(description = "请求时间")
    private long requestTime;

    /**
     * 响应时间戳
     *
     * @see System#currentTimeMillis()
     */
    @Schema(description = "响应时间")
    private long responseTime;

    /**
     * 异常信息,请求对应方法抛出的异常
     */
    @Schema(description = "异常栈信息")
    private String exception;

    public static SerializableAccessLog of(AccessLoggerInfo info) {
        SerializableAccessLog accessLog = FastBeanCopier.copy(info, new SerializableAccessLog(), "parameters", "method", "target", "exception");
        accessLog.setMethod(info.getMethod().getName());
        accessLog.setTarget(info.getTarget().getName());
        //移除敏感请求头
        accessLog.getHttpHeaders().remove("X_Access_Token");
        accessLog.getHttpHeaders().remove(HttpHeaders.AUTHORIZATION);

        accessLog.setException(info.getException() == null ? ""
            : StringUtils.throwable2String(info.getException()));
        Map<String, Object> newParameter = info.getParameters()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                Object value = e.getValue();
                if (value instanceof FilePart) {
                    return ("file:") + ((FilePart) value).filename();
                }
                String className = value.getClass().getName();
                if (className.startsWith("org.springframework")) {
                    return className;
                }
                return JSON.toJSONString(value);
            }));

        accessLog.setParameters(newParameter);
        return accessLog;
    }
}
