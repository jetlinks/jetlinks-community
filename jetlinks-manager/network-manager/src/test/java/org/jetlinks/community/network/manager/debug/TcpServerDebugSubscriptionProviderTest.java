package org.jetlinks.community.network.manager.debug;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//@WebFluxTest(TcpServerDebugSubscriptionProvider.class)
class TcpServerDebugSubscriptionProviderTest  {

    @Test
    void id() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpServerDebugSubscriptionProvider provider
            = new TcpServerDebugSubscriptionProvider(networkManager);
        String id = provider.id();
        assertNotNull(id);
        String name = provider.name();
        assertNotNull(name);
        String[] topicPattern = provider.getTopicPattern();
        assertNotNull(topicPattern);

    }

    @Test
    void name() {
    }

    @Test
    void getTopicPattern() {
    }


    @Test
    void subscribe() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpServerDebugSubscriptionProvider provider
            = new TcpServerDebugSubscriptionProvider(networkManager);


        TcpServer tcpServer = Mockito.mock(TcpServer.class);
        TcpClient client = Mockito.mock(TcpClient.class);
        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(tcpServer));
        Mockito.when(tcpServer.handleConnection()).thenReturn(Flux.just(client));

        Mockito.when(client.getRemoteAddress()).thenReturn(new InetSocketAddress(8001));
        TcpMessage tcpMessage = new TcpMessage();
        EmptyByteBuf buf = new EmptyByteBuf(ByteBufAllocator.DEFAULT);
        tcpMessage.setPayload(buf);
        Mockito.when(client.subscribe())
            .thenReturn(Flux.just(tcpMessage));

        Mockito.when(client.send(Mockito.any(TcpMessage.class)))
            .thenReturn(Mono.just(true));
        //Mockito.doCallRealMethod().when(client).onDisconnect(Mockito.any(Runnable.class));
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0);
            invocationOnMock.getMethod();
//            System.out.println(method.getName());
            return null;
        }).when(client).onDisconnect(Mockito.any(Runnable.class));

//        Mockito.doCallRealMethod().when()



        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/tcp/server/TCP_SERVICE/_subscribe");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("request","aa");
        request.setParameter(parameter);


        provider.subscribe(request).subscribe();

        parameter.put("response","bb");
        provider.subscribe(request).subscribe();
//            .map(TcpServerDebugSubscriptionProvider.TcpClientMessage::getTypeText)
//            .as(StepVerifier::create)
//            .expectNext("连接")
//            .expectNext("订阅")
//            .verifyComplete();


    }

    @Test
    void tcpClientMessage() {
        TcpServerDebugSubscriptionProvider.TcpClientMessage tcpClientMessage
            = TcpServerDebugSubscriptionProvider.TcpClientMessage.of("string", "test", "test");
        String type = tcpClientMessage.getType();
        assertNotNull(type);
        String typeText = tcpClientMessage.getTypeText();
        assertNotNull(typeText);
        Object data = tcpClientMessage.getData();
        assertNotNull(data);

        TcpClient tcpClient = Mockito.mock(TcpClient.class);
        Mockito.when(tcpClient.getRemoteAddress())
            .thenReturn(new InetSocketAddress(123));
        TcpServerDebugSubscriptionProvider.TcpClientMessage tcpClientMessage1
            = TcpServerDebugSubscriptionProvider.TcpClientMessage.of(tcpClient);
        assertNotNull(tcpClientMessage1);

        TcpServerDebugSubscriptionProvider.TcpClientMessage tcpClientMessage2
            = TcpServerDebugSubscriptionProvider.TcpClientMessage.ofDisconnect(tcpClient);
        assertNotNull(tcpClientMessage2);

        TcpMessage tcpMessage = new TcpMessage();
        tcpMessage.setPayload(new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT));
        TcpServerDebugSubscriptionProvider.TcpClientMessage tcpClientMessage3
            = TcpServerDebugSubscriptionProvider.TcpClientMessage.of(tcpClient, tcpMessage);
        assertNotNull(tcpClientMessage3);

    }
}