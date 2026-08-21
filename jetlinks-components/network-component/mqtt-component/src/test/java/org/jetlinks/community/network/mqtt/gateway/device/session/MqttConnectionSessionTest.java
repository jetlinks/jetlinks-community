package org.jetlinks.community.network.mqtt.gateway.device.session;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.mqtt.server.MqttConnection;
import org.jetlinks.community.network.mqtt.server.MqttPublishing;
import org.jetlinks.community.network.mqtt.server.MqttSubscription;
import org.jetlinks.community.network.mqtt.server.MqttUnSubscription;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.server.mqtt.MqttAuth;
import org.jetlinks.core.device.session.DeviceSessionInfo;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MqttConnectionSession} 连接注册与下行发送回归测试.
 * <p>
 * 核心回归场景:连接注册发生在 CONNACK(accept)之前,此时 vertx 的 isConnected 恒为 false,
 * 注册逻辑不得因 isAlive() 为 false 而静默丢弃连接,否则上行正常而全部下行报"设备连接已断开"。
 */
public class MqttConnectionSessionTest {

    private final DeviceGatewayMonitor monitor = GatewayMonitors.getDeviceGatewayMonitor("unit-test");

    private MqttConnectionSession newSession(MqttConnection connection) {
        return newSession(connection, monitor);
    }

    private MqttConnectionSession newSession(MqttConnection connection,
                                             DeviceGatewayMonitor monitor) {
        return new MqttConnectionSession(
            "device-1", null, DefaultTransport.MQTT, connection, monitor, new FakeSessionManager());
    }

    private MqttMessage mqttMessage() {
        return SimpleMqttMessage
            .builder()
            .topic("battery/v1/device-1/ack")
            .payload(Unpooled.wrappedBuffer(new byte[]{1}))
            .qosLevel(1)
            .build();
    }

    /**
     * 回归:CONNACK 前(isAlive=false)注册的连接必须入会话;accept 后下行必须可达.
     */
    @Test
    public void registerBeforeConnackThenSendShouldWork() {
        FakeMqttConnection connection = new FakeMqttConnection(false);
        MqttConnectionSession session = newSession(connection);

        //模拟网关在会话注册完成后才 accept(发送 CONNACK)
        connection.accept();

        StepVerifier.create(session.send(mqttMessage()))
                    .expectNext(true)
                    .verifyComplete();
        assertEquals(1, connection.published.size());
        assertTrue(session.isAlive());
        assertEquals(1, session.getConnections().size());
    }

    /**
     * 认证期间已断开的连接:注册后首个下行会将其清出会话并报连接断开(原守卫的兜底语义仍成立).
     */
    @Test
    public void sendPurgesDeadConnectionAndFailsWhenEmpty() {
        FakeMqttConnection connection = new FakeMqttConnection(false);
        MqttConnectionSession session = newSession(connection);

        StepVerifier.create(session.send(mqttMessage()))
                    .expectErrorSatisfies(err -> assertInstanceOf(DeviceOperationException.class, err))
                    .verify();
        assertTrue(connection.disconnected);
        assertTrue(session.getConnections().isEmpty());
        assertFalse(session.isAlive());
    }

    /**
     * 多连接会话:存在失活连接时下行仍应送达存活连接.
     */
    @Test
    public void sendUsesAliveConnectionAmongMixed() {
        FakeMqttConnection dead = new FakeMqttConnection(false);
        FakeMqttConnection alive = new FakeMqttConnection(true);
        MqttConnectionSession session = newSession(dead);
        session.registerConnection(alive);

        StepVerifier.create(session.send(mqttMessage()))
                    .expectNext(true)
                    .verifyComplete();
        assertEquals(1, alive.published.size());
        assertEquals(0, dead.published.size());
    }

    /**
     * 会话关闭后注册连接:必须显式 reject(SERVER_UNAVAILABLE)而非静默接受.
     */
    @Test
    public void registerAfterCloseRejectsServerUnavailable() {
        FakeMqttConnection first = new FakeMqttConnection(true);
        MqttConnectionSession session = newSession(first);
        session.close();

        FakeMqttConnection second = new FakeMqttConnection(true);
        session.registerConnection(second);

        assertEquals(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, second.rejectedCode);
        assertFalse(session.getConnections().contains(second));
    }

    /**
     * 正常存活连接注册(如 broker 形态注册已 accept 的连接)行为不变.
     */
    @Test
    public void registerAliveConnectionStillWorks() {
        FakeMqttConnection connection = new FakeMqttConnection(true);
        MqttConnectionSession session = newSession(connection);

        assertNull(connection.rejectedCode);
        StepVerifier.create(session.send(mqttMessage()))
                    .expectNext(true)
                    .verifyComplete();
        assertEquals(1, connection.published.size());
    }

    @Test
    public void downstreamMonitorShouldWrapSender() {
        FakeMqttConnection connection = new FakeMqttConnection(true);
        RuntimeException error = new RuntimeException("rejected by monitor");
        AtomicInteger calls = new AtomicInteger();
        DeviceGatewayMonitor monitor = new DeviceGatewayMonitor() {
            @Override
            public Mono<Void> downstream(ClientConnection actualConnection,
                                         DeviceSession session,
                                         EncodedMessage origin,
                                         Mono<Void> sender) {
                assertSame(connection, actualConnection);
                calls.incrementAndGet();
                return Mono.error(error);
            }
        };
        MqttConnectionSession session = newSession(connection, monitor);

        StepVerifier
            .create(session.send(mqttMessage()))
            .expectErrorSatisfies(actual -> assertSame(error, actual))
            .verify();

        assertEquals(1, calls.get());
        assertTrue(connection.published.isEmpty());
    }

    /**
     * MQTT 连接测试替身:isAlive 可控,accept() 模拟 CONNACK 后 isConnected=true 的语义.
     */
    static class FakeMqttConnection implements MqttConnection {

        volatile boolean alive;
        volatile boolean disconnected;
        volatile MqttConnectReturnCode rejectedCode;
        final List<MqttMessage> published = new CopyOnWriteArrayList<>();
        final List<Consumer<MqttConnection>> closeListeners = new CopyOnWriteArrayList<>();

        FakeMqttConnection(boolean alive) {
            this.alive = alive;
        }

        @Override
        public String getClientId() {
            return "device-1";
        }

        @Override
        public Optional<MqttAuth> getAuth() {
            return Optional.empty();
        }

        @Override
        public void reject(MqttConnectReturnCode code) {
            this.rejectedCode = code;
        }

        @Override
        public MqttConnection accept() {
            this.alive = true;
            return this;
        }

        @Override
        public Optional<MqttMessage> getWillMessage() {
            return Optional.empty();
        }

        @Override
        public Flux<MqttPublishing> handleMessage() {
            return Flux.never();
        }

        @Override
        public Mono<Void> publish(MqttMessage message) {
            published.add(message);
            return Mono.empty();
        }

        @Override
        public Flux<MqttSubscription> handleSubscribe(boolean autoAck) {
            return Flux.never();
        }

        @Override
        public Flux<MqttUnSubscription> handleUnSubscribe(boolean autoAck) {
            return Flux.never();
        }

        @Override
        public void onClose(Consumer<MqttConnection> listener) {
            closeListeners.add(listener);
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Mono<Void> close() {
            return Mono.fromRunnable(this::doClose);
        }

        @Override
        public long getLastPingTime() {
            return System.currentTimeMillis();
        }

        @Override
        public void keepAlive() {
        }

        @Override
        public Duration getKeepAliveTimeout() {
            return Duration.ofSeconds(60);
        }

        @Override
        public void setKeepAliveTimeout(Duration duration) {
        }

        @Override
        public InetSocketAddress getClientAddress() {
            return InetSocketAddress.createUnresolved("127.0.0.1", 1883);
        }

        @Override
        public InetSocketAddress address() {
            return getClientAddress();
        }

        @Override
        public Mono<Void> sendMessage(EncodedMessage message) {
            return Mono.empty();
        }

        @Override
        public Flux<EncodedMessage> receiveMessage() {
            return Flux.never();
        }

        @Override
        public void disconnect() {
            this.disconnected = true;
            doClose();
        }

        private void doClose() {
            this.alive = false;
            for (Consumer<MqttConnection> listener : closeListeners) {
                listener.accept(this);
            }
        }
    }

    /**
     * 会话管理器测试替身:仅满足连接注销路径的 getSession 调用.
     */
    static class FakeSessionManager implements DeviceSessionManager {

        @Override
        public String getCurrentServerId() {
            return "unit-test";
        }

        @Override
        public Mono<DeviceSession> compute(String deviceId,
                                           Function<Mono<DeviceSession>, Mono<DeviceSession>> computer) {
            return Mono.empty();
        }

        @Override
        public Mono<DeviceSession> compute(String deviceId,
                                           Mono<DeviceSession> creator,
                                           Function<DeviceSession, Mono<DeviceSession>> updater) {
            return Mono.empty();
        }

        @Override
        public Mono<DeviceSession> getSession(String deviceId) {
            return Mono.empty();
        }

        @Override
        public Mono<DeviceSession> getSession(String deviceId, boolean unregisterWhenNotAlive) {
            return Mono.empty();
        }

        @Override
        public Flux<DeviceSession> getSessions() {
            return Flux.empty();
        }

        @Override
        public Mono<Long> remove(String deviceId, boolean onlyLocal) {
            return Mono.just(0L);
        }

        @Override
        public Mono<Long> remove(String deviceId, Predicate<DeviceSession> predicate) {
            return Mono.just(0L);
        }

        @Override
        public Mono<Boolean> isAlive(String deviceId, boolean onlyLocal) {
            return Mono.just(false);
        }

        @Override
        public Mono<Boolean> checkAlive(String deviceId, boolean onlyLocal) {
            return Mono.just(false);
        }

        @Override
        public Mono<Long> totalSessions(boolean onlyLocal) {
            return Mono.just(0L);
        }

        @Override
        public Flux<DeviceSessionInfo> getSessionInfo() {
            return Flux.empty();
        }

        @Override
        public Flux<DeviceSessionInfo> getDeviceSessionInfo(String deviceId) {
            return Flux.empty();
        }

        @Override
        public Flux<DeviceSessionInfo> getSessionInfo(String serverId) {
            return Flux.empty();
        }

        @Override
        public Flux<DeviceSessionInfo> getLocalSessionInfo() {
            return Flux.empty();
        }

        @Override
        public Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler) {
            return Disposables.disposed();
        }
    }
}
