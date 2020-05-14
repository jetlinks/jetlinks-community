package org.jetlinks.community.network.manager.debug;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class TcpServerDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public TcpServerDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-tcp-server-debug";
    }

    @Override
    public String name() {
        return "TCP服务调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/tcp/server/*/_subscribe"
        };
    }

    @Override
    public Flux<TcpClientMessage> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];

        return subscribe(id, request);
    }

    @SuppressWarnings("all")
    public Flux<TcpClientMessage> subscribe(String id, SubscribeRequest request) {

        String message = request.getString("response").filter(StringUtils::hasText).orElse(null);

        byte[] payload = DebugUtils.stringToBytes(message);

        return Flux.create(sink ->
            sink.onDispose(networkManager
                .<TcpServer>getNetwork(DefaultNetworkType.TCP_SERVER, id)
                .flatMap(server ->
                    server
                        .handleConnection()
                        .doOnNext(client -> sink.next(TcpClientMessage.of(client)))
                        .flatMap(client -> {
                            client.onDisconnect(() -> {
                                sink.next(TcpClientMessage.ofDisconnect(client));
                            });
                            return client
                                .subscribe()
                                .map(msg -> TcpClientMessage.of(client, msg))
                                .doOnNext(sink::next)
                                .flatMap(msg -> {
                                    if (payload.length > 0) {
                                        return client.send(new TcpMessage(Unpooled.wrappedBuffer(payload)));
                                    }
                                    return Mono.empty();
                                })
                                .then();
                        })
                        .then()
                )
                .doOnError(sink::error)
                .subscriberContext(sink.currentContext())
                .subscribe()
            ));
    }


    @AllArgsConstructor(staticName = "of")
    @Getter
    @Setter
    public static class TcpClientMessage {
        private String type;

        private String typeText;

        private Object data;

        public static TcpClientMessage of(TcpClient client) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", client.getRemoteAddress());

            return TcpClientMessage.of("connection", "连接", data);
        }

        public static TcpClientMessage ofDisconnect(TcpClient client) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", client.getRemoteAddress());

            return TcpClientMessage.of("disconnection", "断开连接", data);
        }

        public static TcpClientMessage of(TcpClient connection, TcpMessage message) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", connection.getRemoteAddress().toString());
            data.put("message", message.toString());

            return TcpClientMessage.of("publish", "订阅", data);
        }


    }
}
