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
package org.jetlinks.community.gateway.monitor;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

/**
 * 设备网关监控扩展点。
 *
 * <p>由设备网关在连接生命周期、报文解码和消息上下行阶段调用。实现可记录指标、拒绝连接或报文，
 * 也可按注册顺序包装响应式处理链；实现不得在调用线程中阻塞或主动订阅传入的发布者。</p>
 *
 * @see DeviceGatewayMonitorSupplier
 * @see GatewayMonitors
 * @since 1.0
 */
public interface DeviceGatewayMonitor {

    /**
     * 上报总连接数
     *
     * @param total 总连接数
     * @deprecated 使用 {@link #connected(ClientConnection)} 和 {@link #disconnected(ClientConnection)} 监听连接生命周期
     */
    @Deprecated
    default void totalConnection(long total) {

    }

    /**
     * 创建新连接
     */
    default void connected() {

    }

    /**
     * 拒绝连接
     */
    default void rejected() {

    }

    /**
     * 断开连接
     */
    default void disconnected() {

    }

    /**
     * 网关接收消息
     */
    default void receivedMessage() {

    }

    /**
     * 网关发送消息
     */
    default void sentMessage() {

    }

    /**
     * 客户端长连接建立时执行。
     *
     * <p>默认兼容调用 {@link #connected()}。返回 {@code false} 时网关将拒绝本次连接；
     * 多个监控实现会全部执行，并合并各自的允许结果。</p>
     *
     * @param connection 新建立的客户端连接
     * @return {@code true} 允许连接，{@code false} 拒绝连接
     * @since 2.12
     */
    default boolean connected(ClientConnection connection) {
        connected();
        return true;
    }

    /**
     * 客户端长连接断开时执行。
     *
     * @param connection 已断开的客户端连接
     * @since 2.12
     */
    default void disconnected(ClientConnection connection) {
        disconnected();
    }

    /**
     * 客户端连接被拒绝时执行，例如 MQTT 认证失败或 TCP 长时间未解析出设备。
     *
     * @param connection 被拒绝的客户端连接
     * @param error      拒绝原因；无关联异常时为 {@code null}
     * @since 2.12
     */
    default void rejected(ClientConnection connection, @Nullable Throwable error) {
        rejected();
    }

    /**
     * 原始报文进入协议解码前执行。
     *
     * <p>短连接传输可能不提供连接对象。返回 {@code false} 时本次报文会被丢弃，且不会进入协议解码。</p>
     *
     * @param connection 客户端连接，短连接场景可能为 {@code null}
     * @param message    待解码的原始报文
     * @return {@code true} 继续解码，{@code false} 丢弃报文
     * @since 2.12
     */
    default boolean beforeDecode(@Nullable ClientConnection connection,
                                 EncodedMessage message) {
        return true;
    }

    /**
     * 包装设备上行报文的协议解码任务。
     *
     * <p>实现应返回基于 {@code decoder} 组合出的发布者，保留原链路的背压、取消和错误信号，
     * 不得在方法内主动订阅。</p>
     *
     * @param connection 客户端连接，短连接场景可能为 {@code null}
     * @param session    当前设备会话
     * @param origin     原始报文
     * @param decoder    协议解码任务
     * @return 包装后的解码任务
     * @since 2.12
     */
    default Flux<DeviceMessage> decode(@Nullable ClientConnection connection,
                                       DeviceSession session,
                                       EncodedMessage origin,
                                       Flux<DeviceMessage> decoder) {
        return decoder;
    }

    /**
     * 包装解码完成后、发送到平台前的上行处理任务。
     *
     * <p>{@code handler} 已包含平台消息处理逻辑。实现应保留其背压、取消和错误信号，
     * 不得在方法内主动订阅。</p>
     *
     * @param connection 客户端连接，短连接场景可能为 {@code null}
     * @param session    当前设备会话
     * @param origin     原始报文
     * @param handler    上行平台处理任务
     * @return 包装后的上行处理任务
     * @since 2.12
     */
    default Flux<DeviceMessage> beforeSendToPlatform(@Nullable ClientConnection connection,
                                                     DeviceSession session,
                                                     EncodedMessage origin,
                                                     Flux<DeviceMessage> handler) {
        return handler;
    }

    /**
     * 包装平台消息下发到设备的发送任务。
     *
     * <p>实现应返回基于 {@code sender} 组合出的发布者，并保留原任务的取消和错误信号，
     * 不得在方法内主动订阅。</p>
     *
     * @param connection 当前客户端连接
     * @param session    当前设备会话
     * @param origin     待发送的原始报文
     * @param sender     原始发送任务
     * @return 包装后的发送任务
     * @since 2.12
     */
    default Mono<Void> downstream(ClientConnection connection,
                                  DeviceSession session,
                                  EncodedMessage origin,
                                  Mono<Void> sender) {
        return sender;
    }

}
