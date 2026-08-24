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
package org.jetlinks.community.network.mqtt.gateway.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.jetlinks.core.device.*;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.principal.AuthenticationPrincipal;
import org.jetlinks.core.principal.Identity;
import org.jetlinks.core.principal.PasswordCredential;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttConnectionSession;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.reactivestreams.Subscription;
import org.springframework.util.StringUtils;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.jetlinks.community.network.mqtt.gateway.device.MqttServerDeviceGateway.clientId;

@Slf4j
public class DeviceMqttConnection extends Mono<Void>
    implements Subscription, Consumer<org.jetlinks.community.network.mqtt.server.MqttConnection>, DeviceGatewayContext,
    Function<DeviceMessage, Mono<Void>>, Identity, PasswordCredential, AuthenticationPrincipal {

    private final DeviceGatewayHelper helper;
    private final DeviceGatewayMonitor monitor;
    private final MqttConnection connection;
    private final Disposable.Composite disposable = Disposables.composite();
    private DeviceSession session;
    private CoreSubscriber<? super Void> actual;

    public DeviceMqttConnection(DeviceGatewayHelper helper,
                                DeviceGatewayMonitor monitor,
                                MqttConnection connection) {
        this.helper = helper;
        this.connection = connection;
        this.monitor = monitor;
        this.connection.onClose(this);
    }

    void doAuth() {
        try {
            MqttAuth auth = connection.getAuth().orElse(null);
            if (auth == null || !StringUtils.hasText(connection.getClientId())) {
                reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
                monitor.rejected(connection, null);
            } else {
                doAuth(auth);
            }
        } catch (Throwable e) {
            log.warn("handle mqtt connection failed {}", connection, e);
            reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        }
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public @Nonnull String getType() {
        return DefaultTransport.MQTT.getId();
    }

    @Override
    public @Nonnull String getIdentifier() {
        return connection.getClientId();
    }

    @Override
    public Identity identity() {
        return this;
    }

    @Override
    public PasswordCredential credential() {
        return this;
    }

    @Override
    public String getUsername() {
        return connection
            .getAuth()
            .map(MqttAuth::getUsername)
            .orElse(null);
    }

    @Override
    public char[] getPassword() {
        return connection
            .getAuth()
            .map(auth -> auth.getPassword() == null ? null : auth.getPassword().toCharArray())
            .orElse(null);
    }

    protected DeviceSession wrapSession(MqttConnectionSession session) {
        return session;
    }

    void handleAuth(DeviceOperator device, AuthenticationResponse response) {
        if (response.isSuccess()) {
            //connection.accept();
            String deviceId = device.getDeviceId();
            helper
                .getSessionManager()
                .compute(
                    deviceId,
                    Mono.fromSupplier(() -> wrapSession(
                        new MqttConnectionSession(deviceId, device, getTransport(), connection, monitor, helper.getSessionManager()))),
                    session -> {
                        if (!session.isWrapFrom(MqttConnectionSession.class)) {
                            //如果会话不是MqttConnectionSession,则替换为新的MqttConnectionSession
                            return Mono.just(
                                wrapSession(new MqttConnectionSession(deviceId, device, getTransport(), connection, monitor, helper.getSessionManager()))
                            );
                        } else {
                            session
                                .unwrap(MqttConnectionSession.class)
                                .registerConnection(connection);
                        }
                        return Mono.just(session);
                    })
                //会话empty说明注册会话失败?
                .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)))
                .subscribe(
                    session -> {
                        try {
                            connection.accept();
                            this.session = session;
                            handleMessage();
                        } catch (Throwable ignore) {
                            cancel();
                        }
                    }, error -> {
                        log.warn("register mqtt session failed {}", connection, error);
                        reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    },
                    null,
                    actual.currentContext());
        } else {
            reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
        }
    }

    void handleMessage() {
        if (disposable.isDisposed()) {
            return;
        }
        disposable.add(
            handleClientConnect()
                .thenMany(connection.handleMessage())
                .concatMap(this::decodeAndHandleMessage, 0)
                .subscribe(null, null, null, actual.currentContext())
        );
    }

    protected Mono<Void> handleClientConnect() {
        DeviceOperator operator = session.getOperator();
        if (operator == null) {
            return Mono.empty();
        }
        return operator
            .getProtocol()
            .flatMap(protocol -> protocol.onClientConnect(DefaultTransport.MQTT, connection, this))
            .doOnError(error -> {
                log.warn("处理设备[{}]连接事件失败", session.getDeviceId(), error);
                cancel();
            });
    }

    //解码消息并处理
    protected Mono<Void> decodeAndHandleMessage(MqttMessage message) {
        DeviceOperator operator = session.getOperator();
        if (operator == null) {
            return Mono.empty();
        }

        if (!monitor.beforeDecode(connection, message)) {
            return Mono.empty();
        }

        // 上下文
        FromDeviceMessageContext context =
            FromDeviceMessageContext
                .of(session,
                    message,
                    helper.getRegistry(),
                    connection,
                    this);

        Flux<DeviceMessage> decodeTask = operator
            .getProtocol()
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            //解码
            .flatMapMany(codec -> codec.decode(context))
            .cast(DeviceMessage.class);

        decodeTask = monitor.handleUpstream(
            connection,
            session,
            message,
            decodeTask,
            task -> task
                .concatMap(this::handleMessage, 0)
                .doOnComplete(() -> {
                    if (message instanceof MqttPublishing) {
                        ((MqttPublishing) message).acknowledge();
                    }
                })
        );
        decodeTask = monitor.decode(connection, session, message, decodeTask);

        return decodeTask
            .as(FluxTracer
                    .create(DeviceTracer.SpanName.decode0(operator.getDeviceId()),
                            (span) -> span
                                .setAttributeLazy(DeviceTracer.SpanKey.message, message, Object::toString)
                    ))
            //发生错误不中断流
            .onErrorComplete((err) -> {
                if (message instanceof MqttPublishing) {
                    ((MqttPublishing) message).acknowledge(MqttProperties.NO_PROPERTIES);
                }
                if (err instanceof AccessDenyException) {
                    log.info("handle mqtt message [{}] error:{}", operator.getDeviceId(), message, err);
                } else {
                    log.error("handle mqtt message [{}] error:{}", operator.getDeviceId(), message, err);
                }
                return true;
            })
            .then()
            .subscribeOn(Schedulers.parallel());
    }

    private Mono<DeviceMessage> handleMessage(DeviceMessage message) {
        monitor.receivedMessage();

        DeviceOperator mainDevice = session.getOperator();

        //回填deviceId,有的场景协议包不能或者没有解析出deviceId,则直接使用连接对应的设备id进行填充.
        if (mainDevice != null
            && !StringUtils.hasText(message.getDeviceId())
            && mainDevice.getDeviceId() != null) {
            message.thingId(DeviceThingType.device, mainDevice.getDeviceId());
        }

        //连接已经断开,直接处理消息,不再处理会话
        //有的场景下，设备发送了消息,立即就断开了连接,这是会话已经失效了,如果还继续创建会话的话会出现多次上线的问题.
        if (!connection.isAlive()) {
            return helper
                .getMessageHandler()
                .handleMessage(mainDevice, message)
                .thenReturn(message);
        }
        //统一处理解码后的设备消息
        return helper
            .handleDeviceMessage(
                message,
                device -> wrapSession(
                    new MqttConnectionSession(
                        device.getDeviceId(),
                        device,
                        getTransport(),
                        connection,
                        monitor,
                        helper.getSessionManager())
                ),
                session -> {

                },
                () -> log.warn("无法从MQTT[{}]消息中获取设备信息:{}", connection.getClientId(), message))
            .thenReturn(message);
    }

    void doAuth(MqttAuth auth) {
        MqttAuthenticationRequest request = new MqttAuthenticationRequest(
            connection.getClientId(),
            auth.getUsername(),
            auth.getPassword(),
            DefaultTransport.MQTT);

        helper
            .getRegistry()
            .resolveDevice(this)
            .flatMap(principal -> {
                // 平台统一验证了
                if (principal.isVerified()) {
                    return Mono.just(
                        Tuples.of(
                            AuthenticationResponse.success(),
                            principal.getDevice()
                        )
                    );
                }
                // 交给协议包处理
                return principal
                    .getDevice()
                    .authenticate(request)
                    //如果认证结果返回空,说明协议没有设置认证,或者认证返回不对,默认返回BAD_USER_NAME_OR_PASSWORD,防止由于协议编写不当导致mqtt任意访问的安全问题.
                    .zipWhen(resp -> {
                        //认证响应可以自定义设备ID,如果没有则使用获取到的设备id
                        String deviceId = StringUtils.hasText(resp.getDeviceId()) ? resp.getDeviceId() : principal
                                                                                                         .getDevice()
                                                                                                         .getDeviceId();
                        //认证返回了新的设备ID,则使用新的设备
                        return helper
                            .getRegistry()
                            .getDevice(deviceId);
                    });
            })
            .switchIfEmpty(Mono.fromRunnable(() -> reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)))
            .as(MonoTracer
                    .create(DeviceTracer.SpanName.auth0(connection.getClientId()),
                            (span, tp2) -> {
                                if (!tp2.getT1().isSuccess()) {
                                    span.setStatus(StatusCode.ERROR, tp2.getT1().getMessage());
                                }
                            },
                            (span, hasValue) -> {
                                //empty
                                if (!hasValue) {
                                    span.setStatus(StatusCode.ERROR, "device not exists");
                                }
                                InetSocketAddress address = connection.getClientAddress();
                                if (address != null) {
                                    span.setAttribute(DeviceTracer.SpanKey.address, address.toString());
                                }
                                span.setAttribute(clientId, connection.getClientId());
                            }))
            .subscribe(tp2 -> handleAuth(tp2.getT2(), tp2.getT1()),
                       err -> {
                           if (err instanceof AuthenticationException) {
                               reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                           } else {
                               monitor.rejected(connection, err);
                               log.warn("MQTT连接认证[{}]失败", connection.getClientId(), err);
                               //应答SERVER_UNAVAILABLE
                               reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                           }
                       },
                       null,
                       actual.currentContext());
    }

    private void reject(MqttConnectReturnCode code) {
        if (disposable.isDisposed()) {
            return;
        }
        connection.reject(code);
        cancel();
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Void> actual) {
        synchronized (this) {
            if (disposable.isDisposed()) {
                Operators.complete(actual);
                return;
            }
            if (this.actual != null) {
                Operators.error(actual, new IllegalStateException("already subscribed"));
            }
            this.actual = actual;
            actual.onSubscribe(this);
            this.doAuth();
        }
    }

    @Override
    public void request(long n) {

    }

    @Override
    public void cancel() {
        synchronized (this) {
            if (disposable.isDisposed()) {
                return;
            }
            log.debug("close device mqtt connection [{}]", connection.getClientId());
            disposable.dispose();
            try {
                connection.close().subscribe();
            } catch (Throwable ignore) {
            }
            if (actual != null) {
                actual.onComplete();
            }
            monitor.disconnected(connection);
        }

    }

    @Override
    public void accept(org.jetlinks.community.network.mqtt.server.MqttConnection connection) {
        cancel();
    }

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        return helper.getRegistry().getDevice(deviceId);
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        return helper.getRegistry().getProduct(productId);
    }

    @Override
    public Mono<Void> onMessage(DeviceMessage message) {
        return handleMessage(message).then();
    }

    @Override
    public Mono<Void> apply(DeviceMessage message) {
        return handleMessage(message).then();
    }

}
