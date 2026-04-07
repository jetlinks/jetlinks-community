package org.jetlinks.community.network.mqtt.gateway.device;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.opentelemetry.api.common.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationRequest;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.MessageDecodeContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayHelper;
import org.jetlinks.community.network.mqtt.server.MqttServer;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * MQTT 服务设备网关,用于通过内置的mqtt server来进行设备通信,接入设备到平台中.
 *
 * <pre>
 *     1. 监听Mqtt服务中的连接{@link MqttServer#handleConnection()}
 *     2. 使用{@link org.jetlinks.community.network.mqtt.server.MqttConnection#getClientId()}作为设备ID,从设备注册中心中获取设备.
 *     3. 使用设备对应的协议{@link DeviceOperator#getProtocol()}来进行认证{@link ProtocolSupport#authenticate(AuthenticationRequest, DeviceOperator)}
 *     4. 认证通过后应答mqtt,注册会话{@link DeviceSessionManager#compute(String, Function)}.
 *     5. 监听mqtt消息推送,{@link org.jetlinks.community.network.mqtt.server.MqttConnection#handleMessage()}
 *     6. 当收到消息时,调用对应设备使用的协议{@link ProtocolSupport#getMessageCodec(Transport)}进行解码{@link DeviceMessageCodec#decode(MessageDecodeContext)}
 * </pre>
 *
 * @author zhouhao
 * @see MqttServer
 * @see ProtocolSupport
 * @since 1.0
 */
@Slf4j
public class MqttServerDeviceGateway extends AbstractDeviceGateway {
    static AttributeKey<String> clientId = AttributeKey.stringKey("clientId");
    //Mqtt 服务
    @Getter
    private final MqttServer mqttServer;

    //注销监听器
    private Disposable disposable;

    //设备网关消息处理工具类
    private final DeviceGatewayHelper helper;

    public MqttServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   MqttServer mqttServer,
                                   DecodedClientMessageHandler messageHandler) {
        super(id);
        this.mqttServer = mqttServer;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
    }

    private synchronized void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = mqttServer
            //监听连接,mqtt网关可以复用网络网络组件,多个网关不能收到相同的连接信息
            .handleConnection("device-gateway")
            .filter(conn -> {
                //暂停或者已停止时.
                if (!isStarted()) {
                    //直接响应SERVER_UNAVAILABLE
                    conn.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    monitor.rejected();
                }
                return true;
            })
            //处理mqtt连接请求
            .flatMap(this::handleConnection0, Integer.MAX_VALUE)
            .contextWrite(ctx->ctx.put(DeviceGateway.class, this))
            .subscribe();

    }

    protected Mono<Void> handleConnection0(org.jetlinks.community.network.mqtt.server.MqttConnection connection) {
        return new DeviceMqttConnection(helper,monitor,connection);
    }

    @Override
    protected Mono<Void> doStartup() {
        doStart();
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doShutdown() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        return Mono.empty();
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }
}
