/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
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

import lombok.AllArgsConstructor;
import org.jetlinks.community.log.LogRecord;
import org.jetlinks.community.monitor.AbstractEventMonitor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.monitor.Monitor;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * 协议监控相关辅助工具类. 用于创建协议监控器{@link Monitor}、订阅监控日志数据{@link ProtocolMonitorHelper#subscribeDeviceLog(String, String)}等操作.
 *
 * @author zhouhao
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


    private final EventBus eventBus;

    /**
     * 创建针对协议的监控器
     *
     * @param protocolId 协议ID
     * @return 协议监控器
     */
    public Monitor createMonitor(String protocolId) {
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
        return new ProtocolMonitor(eventBus, protocolId, deviceId);
    }

    private static final SharedPathString ALL_PROTOCOL_LOGGER =
        SharedPathString.of("/_monitor/protocol/*/logger");

    private static final SharedPathString ALL_PROTOCOL_DEVICE_LOGGER =
        SharedPathString.of("/_monitor/protocol/*/device/*/logger");

    static final SharedPathString protocolTracePrefix = SharedPathString.of("/protocol/*");
    static final SharedPathString deviceTracePrefix = SharedPathString.of("/protocol/*/*");

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


}
