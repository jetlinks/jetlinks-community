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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class VertxMqttServer implements MqttServer {

    private final Sinks.Many<MqttConnection> sink = Reactors.createMany(5 * 1024, false);

    private final Map<String, List<Sinks.Many<MqttConnection>>> sinks =
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
        try{
            sink.emitNext(connection,Reactors.emitFailureHandler());
            return true;
        }catch (Throwable ignore){}
        return false;
    }

    private void handleConnection(VertxMqttConnection connection) {
        boolean anyHandled = emitNext(sink, connection);

        for (List<Sinks.Many<MqttConnection>> value : sinks.values()) {
            if (value.size() == 0) {
                continue;
            }
            Sinks.Many<MqttConnection> sink = value.get(ThreadLocalRandom.current().nextInt(value.size()));
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
        List<Sinks.Many<MqttConnection>> sinks = this
            .sinks
            .computeIfAbsent(holder, ignore -> new CopyOnWriteArrayList<>());

        Sinks.Many<MqttConnection> sink = Reactors.createMany(Integer.MAX_VALUE,true);

        sinks.add(sink);

        return sink
            .asFlux()
            .doOnCancel(() -> sinks.remove(sink));
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
