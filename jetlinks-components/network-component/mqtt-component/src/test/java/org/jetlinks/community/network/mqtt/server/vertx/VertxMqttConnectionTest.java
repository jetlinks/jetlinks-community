package org.jetlinks.community.network.mqtt.server.vertx;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.SocketAddress;
import io.vertx.mqtt.MqttEndpoint;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.mqtt.gateway.device.session.MqttConnectionSession;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link VertxMqttConnection#isAlive()} 握手阶段存活语义回归测试.
 * <p>
 * 核心回归场景:vertx 的 {@code MqttEndpoint.isConnected()} 在 CONNACK(accept)之前恒为 false,
 * 而会话注册发生在 accept 之前——注册过程中会话管理器(doRegister)会调用
 * {@code session.getClientAddress()} 触发 takeConnection 的失活清理。
 * 若 isAlive() 在握手阶段误报失活,新连接会被平台自己 disconnect,设备一上线即断开。
 */
public class VertxMqttConnectionTest {

    /**
     * 构造可控的 vertx endpoint 测试替身:
     * isConnected 跟随 connected 标志,accept() 将其置位(模拟 CONNACK 同步生效),
     * publish 立即回调成功.
     */
    private MqttEndpoint mockEndpoint(AtomicBoolean connected) {
        MqttEndpoint endpoint = mock(MqttEndpoint.class, Mockito.RETURNS_SELF);
        when(endpoint.keepAliveTimeSeconds()).thenReturn(60);
        when(endpoint.clientIdentifier()).thenReturn("device-1");
        when(endpoint.isConnected()).thenAnswer(inv -> connected.get());
        when(endpoint.accept()).thenAnswer(inv -> {
            connected.set(true);
            return endpoint;
        });
        when(endpoint.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(51883, "127.0.0.1"));
        when(endpoint.publish(Mockito.any(), Mockito.any(), Mockito.any(),
                              Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyInt(),
                              Mockito.any(), Mockito.any()))
            .thenAnswer(inv -> {
                inv.<Handler<AsyncResult<Integer>>>getArgument(7).handle(Future.succeededFuture(1));
                return endpoint;
            });
        return endpoint;
    }

    /**
     * 核心回归:CONNACK 前(isConnected=false)连接必须存活,否则握手期间会被误清理.
     */
    @Test
    public void preAcceptConnectionMustBeAlive() {
        VertxMqttConnection connection = new VertxMqttConnection(mockEndpoint(new AtomicBoolean(false)));

        assertTrue(connection.isAlive());
    }

    /**
     * 握手期间对端断开:构造器提前挂接的 closeHandler 必须使连接立即失活.
     */
    @Test
    public void closeBeforeAcceptMarksDead() {
        AtomicBoolean connected = new AtomicBoolean(false);
        MqttEndpoint endpoint = mockEndpoint(connected);
        VertxMqttConnection connection = new VertxMqttConnection(endpoint);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Handler<Void>> closeHandler = ArgumentCaptor.forClass(Handler.class);
        verify(endpoint, atLeastOnce()).closeHandler(closeHandler.capture());
        closeHandler.getValue().handle(null);

        assertFalse(connection.isAlive());
    }

    /**
     * accept 后语义与原实现一致:isAlive 跟随 endpoint.isConnected().
     */
    @Test
    public void afterAcceptAliveFollowsEndpointConnected() {
        AtomicBoolean connected = new AtomicBoolean(false);
        VertxMqttConnection connection = new VertxMqttConnection(mockEndpoint(connected));

        connection.accept();
        assertTrue(connection.isAlive());

        //连接断开(未触发 closeHandler 的瞬间)也必须立即失活
        connected.set(false);
        assertFalse(connection.isAlive());
    }

    /**
     * 被拒绝的连接必须失活.
     */
    @Test
    public void rejectMarksDead() {
        VertxMqttConnection connection = new VertxMqttConnection(mockEndpoint(new AtomicBoolean(false)));

        connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);

        assertFalse(connection.isAlive());
    }

    /**
     * 端到端复现生产杀链:注册(CONNACK 前) -> 会话管理器 doRegister 读取 getClientAddress
     * (内部触发 takeConnection 失活清理) -> accept -> 下行发送.
     * 全程连接不得被清出会话,下行必须可达.
     */
    @Test
    public void registrationAddressLookupMustNotKillHandshakingConnection() {
        AtomicBoolean connected = new AtomicBoolean(false);
        VertxMqttConnection connection = new VertxMqttConnection(mockEndpoint(connected));
        MqttConnectionSession session = new MqttConnectionSession(
            "device-1", null, DefaultTransport.MQTT, connection,
            GatewayMonitors.getDeviceGatewayMonitor("unit-test"), null);

        //doRegister: session.getClientAddress().map(InetSocketAddress::toString)...
        assertTrue(session.getClientAddress().isPresent());
        //连接必须仍在会话中且存活(修复前:此处已被 disconnect 并清出会话)
        assertTrue(connection.isAlive());
        assertTrue(session.isAlive());
        assertFalse(session.getConnections().isEmpty());

        connection.accept();

        StepVerifier.create(session.send(SimpleMqttMessage
                                             .builder()
                                             .topic("battery/v1/device-1/ack")
                                             .payload(Unpooled.wrappedBuffer(new byte[]{1}))
                                             .qosLevel(1)
                                             .build()))
                    .expectNext(true)
                    .verifyComplete();
    }
}
