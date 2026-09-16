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
package org.jetlinks.community.network.mqtt.server;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.mqtt.server.MqttServer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.util.concurrent.Queues;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class DefaultJetlinksMqttServer implements JetlinksMqttServer {

    private static final VarHandle DISPOSED;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            DISPOSED = lookup.findVarHandle(DefaultJetlinksMqttServer.class, "disposed", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Sinks.Many<MqttConnection> sink = Reactors.createMany(5 * 1024, false);

    private final Map<String, List<Sinks.Many<MqttConnection>>> sinks = new NonBlockingHashMap<>();

    private DisposableServer server;

    private final String id;

    private boolean disposed = false;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bind;

    @Setter
    private MqttServer reactorServer;

    @Setter
    private JetlinksMqttAuthenticator authenticator;

    public DefaultJetlinksMqttServer(String id) {
        this.id = id;
    }

    public Mono<Void> start() {
        if (reactorServer == null) {
            return Mono.error(new IllegalStateException("Reactor MQTT server is not configured"));
        }

        return reactorServer
            .handle(serverConnection -> {
                JetlinksMqttConnection connection = null;
                if (authenticator != null) {
                    connection = authenticator.getConnection(serverConnection);
                }
                if (connection == null) {
                    // 如果没有配置认证器或认证器中没有该连接，则创建新的连接
                    connection = new JetlinksMqttConnection(serverConnection);
                }
                handleConnection(connection);
                return Mono.empty();
            })
            .bind()
            .doOnSuccess(disposableServer -> {
                this.server = disposableServer;
                // 重置 disposed 标志，以便 shutdown 能够正常工作
                DISPOSED.setVolatile(this, false);
                log.debug("startup mqtt server [{}] on port: {}", id, disposableServer.port());
            })
            .doOnError(err -> {
                this.lastError = err.getMessage();
                log.warn("startup mqtt server [{}] error", id, err);
            })
            .then();
    }

    private boolean emitNext(Sinks.Many<MqttConnection> sink, JetlinksMqttConnection connection) {
        if (sink.currentSubscriberCount() <= 0) {
            return false;
        }
        try {
            return sink.tryEmitNext(connection).isSuccess();
        } catch (Throwable ignore) {
        }
        return false;
    }

    private void handleConnection(JetlinksMqttConnection connection) {
        boolean anyHandled = emitNext(sink, connection);

        for (List<Sinks.Many<MqttConnection>> value : sinks.values()) {
            if (value.isEmpty()) {
                continue;
            }
            Sinks.Many<MqttConnection> s = value.get(ThreadLocalRandom.current().nextInt(value.size()));
            if (emitNext(s, connection)) {
                anyHandled = true;
            }
        }
        if (!anyHandled) {
            connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
        }
    }

    @Override
    public Flux<MqttConnection> handleConnection() {
        return sink.asFlux();
    }

    @Override
    public Flux<MqttConnection> handleConnection(String holder) {
        List<Sinks.Many<MqttConnection>> holderSinks = this.sinks
            .computeIfAbsent(holder, ignore -> new CopyOnWriteArrayList<>());

        Sinks.Many<MqttConnection> holderSink = Sinks.unsafe()
                                                     .many()
                                                     .unicast()
                                                     .onBackpressureBuffer(Queues
                                                                               .<MqttConnection>unboundedMultiproducer()
                                                                               .get());

        holderSinks.add(holderSink);

        return holderSink
            .asFlux()
            .doOnCancel(() -> holderSinks.remove(holderSink));
    }

    @Override
    public boolean isAlive() {
        return server != null && !((boolean) DISPOSED.getVolatile(this));
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Override
    public void shutdown() {
        if (DISPOSED.compareAndSet(this, false, true)) {
            // 完成所有 sink，通知订阅者服务器关闭
            sink.tryEmitComplete();
            for (List<Sinks.Many<MqttConnection>> holderSinks : sinks.values()) {
                for (Sinks.Many<MqttConnection> holderSink : holderSinks) {
                    holderSink.tryEmitComplete();
                }
            }
            sinks.clear();

            if (server != null) {
                try {
                    server.dispose();
                    log.debug("mqtt server [{}] closed", id);
                } catch (Exception e) {
                    log.error("close mqtt server [{}] error", id, e);
                }
                server = null;
            }
        }
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bind;
    }
}
