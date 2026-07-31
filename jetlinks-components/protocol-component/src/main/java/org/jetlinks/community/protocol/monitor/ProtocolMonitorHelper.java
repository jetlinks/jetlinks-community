/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.protocol.monitor;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetlinks.community.log.LogRecord;
import org.jetlinks.community.monitor.AbstractEventMonitor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;
import org.slf4j.LoggerFactory;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 协议监控辅助工具，用于创建协议级或设备级 {@link Monitor}、订阅协议日志，
 * 以及在同步和响应式执行链路中临时切换设备 Monitor。
 *
 * <p>由协议组件创建并注入协议 {@code ServiceContext}。代理 Monitor 只负责选择当前协议
 * Monitor，不采集网关或网络运行指标；跨线程恢复由
 * {@link ProtocolMonitorThreadLocalAccessor} 完成。</p>
 *
 * @author zhouhao
 * @see ProtocolMonitorThreadLocalAccessor
 * @since 2.10
 */
@AllArgsConstructor
public class ProtocolMonitorHelper {

    /**
     * 创建针对某个设备的协议链路Span前缀，可通过此前缀订阅协议包中自定义的设备链路追踪数据{@link org.jetlinks.core.trace.data.SpanDataInfo}.
     *
     * @param protocolId 协议ID
     * @param deviceId   设备ID
     * @return 设备链路追踪Span前缀
     * @see EventBus#subscribe(Subscription)
     */
    public static SeparatedCharSequence createDeviceTraceSpanPrefix(String protocolId, String deviceId) {
        return deviceTracePrefix.replace(2, protocolId, 3, deviceId);
    }

    /**
     * 创建针对某个协议中的日志topic，可通过此前缀订阅协议包中的日志数据{@link LogRecord}.
     *
     * @param protocolId 协议ID
     * @param level      日志级别
     * @return 设备协议日志topic
     */
    public static SeparatedCharSequence createProtocolLoggerTopic(String protocolId, String level) {
        return createProtocolLoggerTopicPrefix(protocolId).append(level);
    }

    /**
     * 创建针对某个设备的协议中的日志topic，可通过此前缀订阅协议包中的日志数据{@link LogRecord}.
     *
     * @param protocolId 协议ID
     * @param deviceId   设备ID
     * @param level      日志级别
     * @return 设备协议日志topic
     */
    public static SeparatedCharSequence createProtocolDeviceLoggerTopic(String protocolId, String deviceId, String level) {
        return createProtocolDeviceLoggerTopicPrefix(protocolId, deviceId).append(level);
    }

    /**
     * 订阅设备协议日志数据.
     *
     * @param deviceId 设备ID
     * @param level    日志级别 * 表示所有
     * @return 日志数据流
     */
    public Flux<LogRecord> subscribeDeviceLog(String deviceId,
                                              String level) {
        return eventBus
            .subscribe(
                Subscription
                    .builder()
                    .subscriberId("protocol-device-logger")
                    .topics(createProtocolDeviceLoggerTopic("*", deviceId, level))
                    .local()
                    .broker()
                    .build(),
                LogRecord.class
            );
    }

    /**
     * 订阅指定协议及该协议下全部设备的日志。
     *
     * @param protocolId 协议ID
     * @param level      日志级别，{@code *}表示全部级别
     * @return 协议日志流，取消订阅时由事件总线释放订阅关系
     * @since 2.12
     */
    public Flux<LogRecord> subscribeProtocolLog(String protocolId,
                                                String level) {
        return eventBus
            .subscribe(
                Subscription
                    .builder()
                    .subscriberId("protocol-logger")
                    .topics(createProtocolLoggerTopic(protocolId, level),
                            createProtocolDeviceLoggerTopic(protocolId, "*", level))
                    .local()
                    .broker()
                    .build(),
                LogRecord.class
            );
    }


    private final EventBus eventBus;

    /**
     * 创建针对协议的监控器
     *
     * @param protocolId 协议ID
     * @return 协议监控器
     */
    public Monitor createMonitor(String protocolId) {
        return ProxyProtocolMonitor.proxy(new ProtocolMonitor(eventBus, protocolId));
    }

    /**
     * 创建不受当前诊断上下文覆盖的协议 Monitor。
     *
     * @param protocolId 协议ID
     * @return 固定归属于指定协议的 Monitor
     * @since 2.12
     */
    public Monitor createMonitorNoProxy(String protocolId) {
        return new ProtocolMonitor(eventBus, protocolId);
    }

    /**
     * 创建针对某个设备的协议监控器
     *
     * @param protocolId 协议ID
     * @param deviceId   设备ID
     * @return 设备协议监控器
     */
    public Monitor createMonitor(String protocolId, String deviceId) {
        return ProxyProtocolMonitor.proxy(new ProtocolMonitor(eventBus, protocolId, deviceId));
    }

    /**
     * 创建不受当前诊断上下文覆盖的设备协议 Monitor。
     *
     * @param protocolId 协议ID
     * @param deviceId   设备ID
     * @return 固定归属于指定协议和设备的 Monitor
     * @since 2.12
     */
    public Monitor createMonitorNoProxy(String protocolId, String deviceId) {
        return new ProtocolMonitor(eventBus, protocolId, deviceId);
    }

    /**
     * @return 当前执行线程绑定的协议 Monitor，不存在时返回 {@code null}
     * @since 2.12
     */
    public static Monitor getCurrentMonitor() {
        return CURRENT_MONITOR.getIfExists();
    }

    /**
     * 清除当前执行线程绑定的协议 Monitor。
     *
     * @since 2.12
     */
    public static void resetCurrentMonitor() {
        CURRENT_MONITOR.remove();
    }

    /**
     * 将协议 Monitor 绑定到当前执行线程；传入 {@code null} 时清除绑定。
     *
     * @param current 当前诊断使用的 Monitor
     * @since 2.12
     */
    public static void makeCurrentMonitor(Monitor current) {
        if (current == null) {
            resetCurrentMonitor();
        } else {
            CURRENT_MONITOR.set(current);
        }
    }

    /**
     * 在指定 Monitor 上下文中同步执行任务，并在完成或异常后恢复此前上下文。
     *
     * @param current  本次执行使用的 Monitor
     * @param callable 同步任务
     * @param <T>      返回值类型
     * @return 任务返回值
     * @since 2.12
     */
    @SneakyThrows
    public static <T> T executeWith(Monitor current, Callable<T> callable) {
        Monitor previous = getCurrentMonitor();
        try {
            makeCurrentMonitor(current);
            return callable.call();
        } finally {
            makeCurrentMonitor(previous);
        }
    }

    /**
     * 在订阅、信号回调和跨线程执行时使用指定 Monitor，并在每次回调后恢复此前上下文。
     *
     * @param current 本次执行使用的 Monitor
     * @param flux    被包装的数据流
     * @param <T>     流元素类型
     * @return 保留原有信号语义的包装数据流
     * @since 2.12
     */
    public static <T> Flux<T> executeWithFlux(Monitor current, Flux<T> flux) {
        return new MonitorFlux<>(flux)
            .contextWrite(ctx -> ctx.put(ProtocolMonitorThreadLocalAccessor.KEY, current))
            .contextCapture();
    }

    /**
     * 在订阅、信号回调和跨线程执行时使用指定 Monitor，并在每次回调后恢复此前上下文。
     *
     * @param current 本次执行使用的 Monitor
     * @param mono    被包装的单值流
     * @param <T>     流元素类型
     * @return 保留原有完成、空值和异常语义的包装单值流
     * @since 2.12
     */
    public static <T> Mono<T> executeWithMono(Monitor current, Mono<T> mono) {
        return new MonitorMono<>(mono)
            .contextWrite(ctx -> ctx.put(ProtocolMonitorThreadLocalAccessor.KEY, current))
            .contextCapture();
    }

    static <T, R> R doWith(T data, Monitor monitor, BiFunction<T, Monitor, R> mapper) {
        Monitor previous = getCurrentMonitor();
        try {
            makeCurrentMonitor(monitor);
            return mapper.apply(data, monitor);
        } finally {
            makeCurrentMonitor(previous);
        }
    }

    static void doWith(Monitor monitor, Consumer<Monitor> consumer) {
        Monitor previous = getCurrentMonitor();
        try {
            makeCurrentMonitor(monitor);
            consumer.accept(monitor);
        } finally {
            makeCurrentMonitor(previous);
        }
    }

    @AllArgsConstructor
    static class MonitorMono<T> extends Mono<T> {
        private final Mono<T> source;

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            doWith(
                actual,
                actual.currentContext().getOrDefault(ProtocolMonitorThreadLocalAccessor.KEY, null),
                (subscriber, monitor) -> {
                    source.subscribe(new MonitorSwitchSubscriber<>(subscriber));
                    return null;
                }
            );
        }
    }

    @AllArgsConstructor
    static class MonitorFlux<T> extends Flux<T> {
        private final Flux<T> source;

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            doWith(
                actual,
                actual.currentContext().getOrDefault(ProtocolMonitorThreadLocalAccessor.KEY, null),
                (subscriber, monitor) -> {
                    source.subscribe(new MonitorSwitchSubscriber<>(subscriber));
                    return null;
                }
            );
        }
    }

    @AllArgsConstructor
    static class MonitorSwitchSubscriber<T> extends BaseSubscriber<T> {
        private final CoreSubscriber<T> actual;

        @Override
        @Nonnull
        public Context currentContext() {
            return actual.currentContext();
        }

        @Override
        protected void hookOnSubscribe(@Nonnull org.reactivestreams.Subscription subscription) {
            actual.onSubscribe(this);
        }

        private Monitor current() {
            return currentContext()
                .getOrDefault(ProtocolMonitorThreadLocalAccessor.KEY, null);
        }

        @Override
        protected void hookOnComplete() {
            doWith(current(), ignored -> actual.onComplete());
        }

        @Override
        protected void hookOnError(@Nonnull Throwable error) {
            doWith(error, current(), (value, ignored) -> {
                actual.onError(value);
                return null;
            });
        }

        @Override
        protected void hookOnNext(@Nonnull T value) {
            doWith(value, current(), (next, ignored) -> {
                actual.onNext(next);
                return null;
            });
        }
    }

    private static final SharedPathString ALL_PROTOCOL_LOGGER =
        SharedPathString.of("/_monitor/protocol/*/logger");

    private static final SharedPathString ALL_PROTOCOL_DEVICE_LOGGER =
        SharedPathString.of("/_monitor/protocol/*/device/*/logger");

    static final SharedPathString protocolTracePrefix = SharedPathString.of("/protocol/*");
    static final SharedPathString deviceTracePrefix = SharedPathString.of("/protocol/*/*");

    private static final FastThreadLocal<Monitor> CURRENT_MONITOR = new FastThreadLocal<>();

    static SeparatedCharSequence createProtocolLoggerTopicPrefix(String protocolId) {
        return ALL_PROTOCOL_LOGGER.replace(3, protocolId);
    }

    static SeparatedCharSequence createProtocolDeviceLoggerTopicPrefix(String protocolId, String deviceId) {
        return ALL_PROTOCOL_DEVICE_LOGGER
            .replace(3, protocolId, 5, deviceId);
    }


    private static class ProtocolMonitor extends AbstractEventMonitor {
        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(
            "org.jetlinks.protocol.monitor"
        );

        public ProtocolMonitor(EventBus eventBus,
                               String protocolId) {
            super(eventBus,
                  protocolTracePrefix.replace(2, protocolId),
                  createProtocolLoggerTopicPrefix(protocolId));
        }

        public ProtocolMonitor(EventBus eventBus,
                               String protocolId,
                               String deviceId) {
            super(eventBus,
                  createDeviceTraceSpanPrefix(protocolId, deviceId),
                  createProtocolDeviceLoggerTopicPrefix(protocolId, deviceId));
        }


        @Override
        protected CharSequence getLogType() {
            return this.loggerEventPrefix.range(3, this.loggerEventPrefix.size() - 1);
        }

        @Override
        public org.slf4j.Logger getLogger() {
            return logger;
        }
    }

    private static class ProxyProtocolMonitor implements Monitor {
        private Monitor proxy;

        private static ProxyProtocolMonitor proxy(Monitor monitor) {
            ProxyProtocolMonitor proxy = new ProxyProtocolMonitor();
            proxy.proxy = monitor;
            return proxy;
        }

        @Override
        public Logger logger() {
            Monitor current = CURRENT_MONITOR.getIfExists();
            return current == null ? proxy.logger() : current.logger();
        }

        @Override
        public Tracer tracer() {
            Monitor current = CURRENT_MONITOR.getIfExists();
            return current == null ? proxy.tracer() : current.tracer();
        }

        @Override
        public Metrics metrics() {
            Monitor current = CURRENT_MONITOR.getIfExists();
            return current == null ? proxy.metrics() : current.metrics();
        }
    }

}
