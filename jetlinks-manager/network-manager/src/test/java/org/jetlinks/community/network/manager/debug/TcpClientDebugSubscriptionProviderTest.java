package org.jetlinks.community.network.manager.debug;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.impl.NetSocketImpl;
import io.vertx.mqtt.MqttServer;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.test.web.TestAuthentication;
import org.jetlinks.community.network.tcp.client.TcpClientProperties;
import org.jetlinks.community.network.tcp.client.VertxTcpClient;
import org.jetlinks.community.network.tcp.client.VertxTcpClientProvider;
import org.jetlinks.community.network.tcp.parser.DefaultPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.DirectRecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TcpClientDebugSubscriptionProviderTest {

    @Test
    void id() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        String id = provider.id();
        assertNotNull(id);
    }

    @Test
    void name() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        String name = provider.name();
        assertNotNull(name);
    }

    @Test
    void getTopicPattern() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);
    }

    @Test
    void subscribe() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        Vertx vertx = Vertx.vertx();
        MqttServer server = MqttServer.create(vertx);

        server.endpointHandler(endpoint -> {
            endpoint
                .accept()
                .publish("/test", Buffer.buffer("test"), MqttQoS.AT_MOST_ONCE, false, false);
        }).listen(1884);


        VertxTcpClientProvider vertxTcpClientProvider = new VertxTcpClientProvider(id -> Mono.empty(), vertx, new DefaultPayloadParserBuilder());
        TcpClientProperties tcpClientProperties = new TcpClientProperties();
        tcpClientProperties.setHost("127.0.0.1");
        tcpClientProperties.setPort(1884);
        tcpClientProperties.setOptions(new NetClientOptions());
        VertxTcpClient client = vertxTcpClientProvider.createNetwork(tcpClientProperties);
        PayloadParser payloadParser = Mockito.mock(PayloadParser.class);

        BufferImpl buffer = new BufferImpl();
        buffer.appendString("aaa");
        Mockito.when(payloadParser.handlePayload())
            .thenReturn(Flux.just(buffer));
        client.setRecordParser(payloadParser);

        NetSocketImpl socket = Mockito.mock(NetSocketImpl.class);
        Mockito.when(socket.closeHandler(Mockito.any(Handler.class)))
            .thenReturn(socket);
        Mockito.when(socket.handler(Mockito.any(Handler.class)))
            .thenReturn(socket);
        client.setSocket(socket);

        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(client));



        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/tcp/client/TCP_CLIENT/_subscribe");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("response","aa");
        request.setParameter(parameter);
        provider.subscribe(request).subscribe();

        request.setTopic("/network/tcp/client/TCP_CLIENT/_send");
        parameter.put("request","bbb");
        provider.subscribe(request).subscribe();



    }

    @Test
    void send() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        SubscribeRequest request = new SubscribeRequest();
        request.setId("test");
        request.setTopic("/network/tcp/client/TCP_CLIENT/_subscribe");
        Executable executable = ()-> provider.send("test",request);
        assertThrows(IllegalArgumentException.class,executable);
    }

}