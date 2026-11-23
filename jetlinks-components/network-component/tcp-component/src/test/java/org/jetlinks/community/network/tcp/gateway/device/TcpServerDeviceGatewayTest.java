package org.jetlinks.community.network.tcp.gateway.device;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.server.TcpServer;
import org.jetlinks.supports.device.session.LocalDeviceSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TcpServerDeviceGatewayTest {

    private static final String DEVICE_ID = "tcp-session-test";

    private final LocalDeviceSessionManager sessionManager = new LocalDeviceSessionManager();
    private final List<TcpServerDeviceGateway.TcpConnection> connections = new ArrayList<>();
    private final List<DeviceSession> sessionsOnRegister = new ArrayList<>();
    private final AtomicInteger registrations = new AtomicInteger();
    private final AtomicInteger unregistrations = new AtomicInteger();

    private TcpServerDeviceGateway gateway;

    @BeforeEach
    void setUp() {
        DeviceOperator device = mock(DeviceOperator.class);
        when(device.getId()).thenReturn(DEVICE_ID);
        when(device.online(anyString(), any(), anyLong())).thenReturn(Mono.just(true));
        when(device.offline()).thenReturn(Mono.just(true));
        DeviceRegistry registry = mock(DeviceRegistry.class);
        when(registry.getDevice(DEVICE_ID)).thenReturn(Mono.just(device));

        sessionManager.listenEvent(event -> {
            if (event.getType() == DeviceSessionEvent.Type.register) {
                registrations.incrementAndGet();
                return sessionManager
                    .getSession(DEVICE_ID)
                    .doOnNext(sessionsOnRegister::add)
                    .then();
            }
            unregistrations.incrementAndGet();
            return Mono.empty();
        });
        gateway = new TcpServerDeviceGateway(
            "tcp-session-gateway",
            Mono.empty(),
            registry,
            (deviceOperator, message) -> Mono.just(true),
            sessionManager,
            mock(TcpServer.class));
    }

    @AfterEach
    void tearDown() {
        connections.forEach(TcpServerDeviceGateway.TcpConnection::cancel);
        sessionManager.remove(DEVICE_ID, true).block(Duration.ofSeconds(3));
        sessionManager.shutdown();
    }

    @Test
    void shouldKeepSessionAliveWhenOnlineListenerQueriesIt() {
        TcpServerDeviceGateway.TcpConnection connection = newConnection();

        StepVerifier.create(connection.onMessage(message())).verifyComplete();

        DeviceSession session = sessionManager.getSession(DEVICE_ID).block(Duration.ofSeconds(3));
        assertNotNull(session);
        assertTrue(session.isAlive());
        assertSame(session, connection.session);
        assertEquals(List.of(session), sessionsOnRegister);
        assertEquals(1, registrations.get());
        assertEquals(0, unregistrations.get());
        assertIterableEquals(List.of(connection.client), session.unwrap(TcpDeviceSession.class).getConnections());
        verify(connection.client, times(2)).onDisconnect(any());
    }

    @Test
    void shouldAttachNewConnectionToExistingSession() {
        TcpServerDeviceGateway.TcpConnection first = newConnection();
        StepVerifier.create(first.onMessage(message())).verifyComplete();
        DeviceSession original = sessionManager.getSession(DEVICE_ID).block(Duration.ofSeconds(3));
        assertNotNull(original);

        TcpServerDeviceGateway.TcpConnection second = newConnection();
        StepVerifier.create(second.onMessage(message())).verifyComplete();

        DeviceSession session = sessionManager.getSession(DEVICE_ID).block(Duration.ofSeconds(3));
        assertSame(original, session);
        assertSame(session, first.session);
        assertSame(session, second.session);
        assertIterableEquals(List.of(first.client, second.client), session.unwrap(TcpDeviceSession.class).getConnections());
        assertEquals(1, registrations.get());
        assertEquals(0, unregistrations.get());
    }

    private TcpServerDeviceGateway.TcpConnection newConnection() {
        TcpClient client = mock(TcpClient.class, CALLS_REAL_METHODS);
        when(client.isAlive()).thenReturn(true);
        when(client.subscribe()).thenReturn(Flux.never());
        when(client.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        TcpServerDeviceGateway.TcpConnection connection = new TcpServerDeviceGateway.TcpConnection(gateway, client);
        connections.add(connection);
        return connection;
    }

    private ReportPropertyMessage message() {
        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setDeviceId(DEVICE_ID);
        return message;
    }
}
