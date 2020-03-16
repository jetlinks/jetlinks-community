package org.jetlinks.community.network.manager.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@RestController
@RequestMapping("/network/tcp")
@Resource(id = "network-config", name = "网络组件配置")
public class TcpController {

    private final NetworkManager networkManager;

    public TcpController(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @PostMapping("/client/{id}/_send/{type}")
    public Mono<Boolean> send(@PathVariable String id, @PathVariable PayloadType type, @RequestBody Mono<String> data) {
        return networkManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, id)
            .flatMap(client -> data.flatMap(msg -> client.send(new TcpMessage(type.write(msg)))));
    }

    @GetMapping(value = "/client/{id}/_subscribe/{type}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Object> subscribe(@PathVariable String id, @PathVariable PayloadType type) {
        return networkManager.<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, id)
            .flatMapMany(TcpClient::subscribe)
            .map(tcpMessage -> type.read(tcpMessage.getPayload()))
            ;
    }

    @GetMapping(value = "/server/{id}/_subscribe/{type}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TcpClientMessage> serverSubscribe(@PathVariable String id, @PathVariable PayloadType type) {
        return Flux.create(sink -> {
            sink.onDispose(networkManager.<TcpServer>getNetwork(DefaultNetworkType.TCP_SERVER, id)
                .flatMapMany(TcpServer::handleConnection)
                .flatMap(client -> {
                    String address = client.getRemoteAddress().toString();
                    sink.next(new TcpClientMessage(address, "已建立连接"));
                    client.onDisconnect(() -> sink.next(new TcpClientMessage(address, "已断开连接")));
                    return client
                        .subscribe()
                        .map(msg -> new TcpClientMessage(address, type.read(msg.getPayload())));

                })
                .subscriberContext(sink.currentContext())
                .subscribe(sink::next));
        });
    }

    @Getter
    @AllArgsConstructor
    public static class TcpClientMessage {
        private String clientAddress;

        private Object payload;
    }

}
