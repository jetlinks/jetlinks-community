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
package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 由网络提供器管理的 MQTT 服务端，将连接分发给全局订阅者和各 holder 的一个接收者。
 * 订阅变更时原子发布不可变快照，分发在同一次快照中选择接收者，不复制订阅集合。
 * 连接认证和设备会话注册由接收连接的网关负责。
 */
@Slf4j
public class VertxMqttServer implements MqttServer {

    private final Sinks.Many<MqttConnection> sink = Reactors.createMany(5 * 1024, false);

    private final ConcurrentMap<String, List<Sinks.Many<MqttConnection>>> sinks =
        new NonBlockingHashMap<>();

    private Collection<io.vertx.mqtt.MqttServer> mqttServer;

    private final String id;

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bind;

    public VertxMqttServer(String id) {
        this.id = id;
    }

    public void setMqttServer(Collection<io.vertx.mqtt.MqttServer> mqttServer) {
        if (this.mqttServer != null && !this.mqttServer.isEmpty()) {
            shutdown();
        }
        this.mqttServer = mqttServer;
        for (io.vertx.mqtt.MqttServer server : this.mqttServer) {
            server
                .exceptionHandler(error -> {
                    log.error(error.getMessage(), error);
                })
                .endpointHandler(endpoint -> {
                    handleConnection(new VertxMqttConnection(endpoint));
                });
        }
    }

    private boolean emitNext(Sinks.Many<MqttConnection> sink, VertxMqttConnection connection){
        if (sink.currentSubscriberCount() <= 0) {
            return false;
        }
        try {
            return sink.tryEmitNext(connection).isSuccess();
        } catch (Throwable ignore) {
        }
        return false;
    }

    private void handleConnection(VertxMqttConnection connection) {
        boolean anyHandled = emitNext(sink, connection);

        // 快照只在订阅变更时复制；持有旧快照的分发不受并发取消影响。
        for (List<Sinks.Many<MqttConnection>> snapshot : sinks.values()) {
            if (snapshot.isEmpty()) {
                continue;
            }
            Sinks.Many<MqttConnection> sink = snapshot.get(ThreadLocalRandom.current().nextInt(snapshot.size()));
            if (emitNext(sink, connection)) {
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
        Sinks.Many<MqttConnection> sink =
            Sinks.unsafe()
                 .many()
                 .unicast()
                 .onBackpressureBuffer(Queues.<MqttConnection>unboundedMultiproducer().get());

        addConnectionSink(holder, sink);

        return sink
            .asFlux()
            .doOnCancel(() -> removeConnectionSink(holder, sink));
    }

    private void addConnectionSink(String holder, Sinks.Many<MqttConnection> sink) {
        // ConcurrentMap 原子替换不可变列表，避免并发注册、取消覆盖其他订阅。
        sinks.compute(holder, (key, current) -> {
            List<Sinks.Many<MqttConnection>> snapshot =
                current == null ? new ArrayList<>(1) : new ArrayList<>(current);
            snapshot.add(sink);
            return Collections.unmodifiableList(snapshot);
        });
    }

    private void removeConnectionSink(String holder, Sinks.Many<MqttConnection> sink) {
        sinks.computeIfPresent(holder, (key, current) -> {
            List<Sinks.Many<MqttConnection>> snapshot = new ArrayList<>(current);
            snapshot.remove(sink);
            return snapshot.isEmpty() ? null : Collections.unmodifiableList(snapshot);
        });
    }

    @Override
    public boolean isAlive() {
        return mqttServer != null && !mqttServer.isEmpty();
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
        if (mqttServer != null) {
            for (io.vertx.mqtt.MqttServer server : mqttServer) {
                server.close(res -> {
                    if (res.failed()) {
                        log.error(res.cause().getMessage(), res.cause());
                    } else {
                        log.debug("mqtt server [{}] closed", server.actualPort());
                    }
                });
            }
            mqttServer.clear();
        }

    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bind;
    }
}
