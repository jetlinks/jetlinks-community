package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Collection;
import java.util.function.Function;

@Slf4j
public class VertxMqttServer implements MqttServer {

    private EmitterProcessor<MqttConnection> connectionProcessor = EmitterProcessor.create(false);

    FluxSink<MqttConnection> sink = connectionProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private Collection<io.vertx.mqtt.MqttServer> mqttServer;

    private String id;

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
                    if (!connectionProcessor.hasDownstreams()) {
                        log.info("mqtt server no handler for:[{}]", endpoint.clientIdentifier());
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                        return;
                    }
                    if (connectionProcessor.getPending() >= 10240) {
                        log.warn("too many no handle mqtt connection : {}", connectionProcessor.getPending());
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                        return;
                    }
                    sink.next(new VertxMqttConnection(endpoint));
                });
        }
    }


    @Override
    public Flux<MqttConnection> handleConnection() {
        return connectionProcessor
            .map(Function.identity());
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
}
