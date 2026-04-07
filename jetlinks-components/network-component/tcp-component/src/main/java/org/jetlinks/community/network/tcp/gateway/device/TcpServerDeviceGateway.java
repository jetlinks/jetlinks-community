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
package org.jetlinks.community.network.tcp.gateway.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.MonitorSupportDeviceGateway;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
class TcpServerDeviceGateway extends AbstractDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    final TcpServer tcpServer;

    @Getter
    Mono<ProtocolSupport> protocol;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    //连接检查超时时间,超过时间连接没有被正确处理返回会话,将被自动断开连接
    @Setter
    private Duration connectCheckTimeout = TimeUtils.parse(System.getProperty("gateway.tcp.network.connect-check-timeout", "10s"));

    public TcpServerDeviceGateway(String id,
                                  Mono<ProtocolSupport> protocol,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  TcpServer tcpServer) {
        super(id);
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.tcpServer = tcpServer;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }


    static class TcpConnection extends Mono<Void> implements DeviceGatewayContext, Runnable, Subscription {

        final TcpServerDeviceGateway parent;
        final TcpClient client;

        static final AtomicReferenceFieldUpdater<TcpConnection, DeviceSession>
            SESSION = AtomicReferenceFieldUpdater.newUpdater(TcpConnection.class, DeviceSession.class, "session");
        volatile DeviceSession session;

        final InetSocketAddress address;
        Disposable legalityChecker;
        final Disposable.Composite disposable = Disposables.composite();
        MessageParser parser;

        CoreSubscriber<? super Void> subscriber;

        TcpConnection(TcpServerDeviceGateway parent, TcpClient client) {
            this.client = client;
            this.parent = parent;
            this.address = client.getRemoteAddress();
            parent.monitor.totalConnection(parent.counter.sum());
            parent.monitor.connected();
            client.onDisconnect(this);

            legalityChecker = Schedulers
                .parallel()
                .schedule(this::checkLegality, parent.connectCheckTimeout.toMillis(), TimeUnit.MILLISECONDS);
            accept();
        }

        public void checkLegality() {
            //超过时间还未获取到任何设备则认为连接不合法，自动断开连接
            if (session == null) {
                log.info("tcp [{}] connection is illegal, close it.", address);
                try {
                    client.disconnect();
                } catch (Throwable ignore) {
                }
            }
        }

        DeviceSession session() {
            return session == null
                ? new UnknownTcpDeviceSession(client.getId(), client, parent.getTransport(), parent.monitor)
                : session;
        }

        void accept() {
            disposable.add(
                parent
                    .getProtocol()
                    .flatMap(protocol -> protocol
                        .getMessageParser(parent.getTransport())
                        .flatMap(factory -> factory.create(client))
                        .doOnNext(parser -> {
                            this.parser = parser;
                            this.disposable.add(parser);
                        })
                        .then(protocol.onClientConnect(parent.getTransport(), client, this)))
                    .thenMany(client.subscribe().concatMap(this::handleTcpMessage, 0))
                    .subscribe()
            );
        }

        Mono<Void> handleTcpMessage(TcpMessage message) {
            // 协议包自定义了报文解析
            if (parser != null) {
                List<? extends EncodedMessage> messages = parser.handle(message);
                if (messages == null || messages.isEmpty()) {
                    return Mono.empty();
                }
                if (messages.size() == 1) {
                    return handleTcpMessage0(messages.get(0));
                }
                return Flux
                    .fromIterable(messages)
                    .concatMap(this::handleTcpMessage0)
                    .then();
            }
            return handleTcpMessage0(message);
        }

        Mono<Void> handleTcpMessage0(EncodedMessage message) {
            if (!parent.isStarted()) {
                return Mono.empty();
            }
            return parent
                .getProtocol()
                .flatMap(pt -> pt.getMessageCodec(parent.getTransport()))
                .flatMapMany(codec -> codec
                    .decode(FromDeviceMessageContext.of(
                        session(),
                        message,
                        parent.registry,
                        client,
                        msg -> handleDeviceMessage(msg).then())))
                .cast(DeviceMessage.class)
                .concatMap(this::handleDeviceMessage, 0)
                .as(FluxTracer.create(
                    DeviceTracer.SpanName.decode0(session == null ? "unknown" : session.getDeviceId()),
                    builder -> builder
                        .setAttributeLazy(
                            DeviceTracer.SpanKey.message,
                            message,
                            (m) -> message.toString())
                ))
                .onErrorResume((err) -> {
                    log.error("{} Handle TCP[{}] message failed:\n{}",
                              parent.getId(),
                              address,
                              message
                        , err);
                    return Mono.fromRunnable(client::reset);
                })
                .subscribeOn(Schedulers.parallel())
                .then();
        }

        Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
            Disposable checker = legalityChecker;
            if (checker != null) {
                checker.dispose();
                legalityChecker = null;
            }
            parent.monitor.receivedMessage();
            return parent
                .helper
                .handleDeviceMessage(
                    message,
                    device -> new TcpDeviceSession(device, parent.getTransport(), parent.monitor, parent.sessionManager),
                    session -> {
                        if (session.isWrapFrom(TcpDeviceSession.class)) {
                            TcpDeviceSession deviceSession = session.unwrap(TcpDeviceSession.class);
                            deviceSession.registerConnection(client);
                            SESSION.set(this, session);
                        }
                    },
                    () -> log.warn("TCP{}: The device[{}] in the message body does not exist:{}", address, message.getDeviceId(), message)
                )
                .thenReturn(message);
        }

        @Override
        public Mono<DeviceOperator> getDevice(String deviceId) {
            return parent.registry.getDevice(deviceId);
        }

        @Override
        public Mono<DeviceProductOperator> getProduct(String productId) {
            return parent.registry.getProduct(productId);
        }

        @Override
        public Mono<Void> onMessage(DeviceMessage message) {
            return handleDeviceMessage(message).then();
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super Void> actual) {
            try {
                synchronized (disposable) {
                    if (disposable.isDisposed()) {
                        Operators.complete(actual);
                        return;
                    }
                    if (subscriber != null) {
                        Operators.complete(actual);
//                    actual.onError(Exceptions.duplicateOnSubscribeException());
                        return;
                    }

                    this.subscriber = actual;
                    this.subscriber.onSubscribe(this);
                }
            } catch (Throwable error) {
                Operators.complete(actual);
                log.warn("{} handle tcp client {} failed", parent.getId(), client.getRemoteAddress(), error);
                client.disconnect();
            }
        }

        @Override
        public void run() {
            cancel();
        }

        @Override
        public void request(long n) {

        }


        @Override
        public void cancel() {
            synchronized (disposable) {
                if (disposable.isDisposed()) {
                    return;
                }
                disposable.dispose();
            }
            parent.counter.decrement();
            parent.monitor.disconnected();
            parent.monitor.totalConnection(parent.counter.sum());
            if (this.subscriber != null) {
                this.subscriber.onComplete();
            }
            try {
                client.shutdown();
            } catch (Throwable ignore) {

            }
            //check session
            DeviceSession session = this.session;
            if (session != null && session.getDeviceId() != null) {
                parent
                    .sessionManager
                    .getSession(session.getDeviceId())
                    .subscribe();
            }
        }
    }


    private void doStart() {
        if (isStarted() || disposable != null) {
            return;
        }
        disposable = tcpServer
            .handleConnection()
            .publishOn(Schedulers.parallel())
            .flatMap(client -> {
                try {
                    return new TcpConnection(this, client);
                } catch (Throwable e) {
                    try {
                        client.disconnect();
                    } catch (Throwable ignore) {
                    }
                    log.warn("{} handle tcp client {} failed", getId(), client.getRemoteAddress(), e);
                    return Mono.empty();
                }
            }, Integer.MAX_VALUE)
            .contextWrite(ctx -> ctx.put(DeviceGateway.class, this))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    protected Mono<Void> doShutdown() {
        return Mono.fromRunnable(() -> {
            if (null != disposable) {
                disposable.dispose();
                disposable = null;
            }
        });
    }
}
