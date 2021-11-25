package org.jetlinks.community.network.manager.debug;

import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.manager.test.web.TestAuthentication;
import org.jetlinks.community.network.tcp.client.VertxTcpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

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
        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(new VertxTcpClient("TCP_CLIENT",true)));

        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/tcp/client/TCP_CLIENT/_subscribe");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("response","aa");
        request.setParameter(parameter);
        provider.subscribe(request).blockFirst(Duration.ofSeconds(5));

    }

    @Test
    void send() {
        NetworkManager networkManager = Mockito.mock(NetworkManager.class);
        TcpClientDebugSubscriptionProvider provider
            = new TcpClientDebugSubscriptionProvider(networkManager);
        Mockito.when(networkManager.getNetwork(Mockito.any(NetworkType.class),Mockito.anyString()))
            .thenReturn(Mono.just(new VertxTcpClient("TCP_CLIENT",true)));

        SubscribeRequest request = new SubscribeRequest();
        TestAuthentication authentication = new TestAuthentication("test");
        authentication.addPermission("network-config", "save");
        request.setAuthentication(authentication);
        request.setId("test");
        request.setTopic("/network/tcp/client/TCP_CLIENT/_send");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("request","aa");
        request.setParameter(parameter);
        provider.subscribe(request).blockFirst(Duration.ofSeconds(5));
    }

}