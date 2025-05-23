package org.jetlinks.community.plugin.device;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.function.Function;

public class PluginDeviceSession implements PersistentSession {
    private final DeviceOperator device;
    @Setter
    @Getter
    private DeviceGatewayPlugin plugin;

    private long connectTime = System.currentTimeMillis();

    private long pingTime = connectTime;

    private long timeout = -1;

    public PluginDeviceSession(DeviceOperator device) {
        this.device = device;
    }

    @Override
    public String getId() {
        return device.getId();
    }

    @Override
    public String getDeviceId() {
        return device.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return device;
    }

    @Override
    public long lastPingTime() {
        return pingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Reactors.ALWAYS_FALSE;
    }

    @Override
    public Transport getTransport() {
        return PluginTransport.plugin;
    }

    @Override
    public void close() {

    }

    @Override
    public void keepAlive() {
        pingTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.timeout = timeout.toMillis();
    }

    @Override
    public void ping() {
        pingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return timeout <= 0 || System.currentTimeMillis() - pingTime < timeout;
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        //检查真实状态?
        return PersistentSession.super.isAliveAsync();
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public String getProvider() {
        return PluginDeviceGatewayProvider.PROVIDER;
    }

    @SneakyThrows
    void write(ObjectOutput out) {

        out.writeUTF(getDeviceId());
        out.writeLong(connectTime);
        out.writeLong(pingTime);
        out.writeLong(timeout);
        SerializeUtils.writeNullableUTF(plugin == null ? null : plugin.getId(), out);
    }

    @SneakyThrows
    @SuppressWarnings("all")
    static Mono<PluginDeviceSession> read(ObjectInput input,
                                          DeviceRegistry registry,
                                          Function<String, DeviceGatewayPlugin> pluginLoader) {
        String deviceId = input.readUTF();
        long connectTime = input.readLong();
        long pingTime = input.readLong();
        long timeout = input.readLong();
        String pluginId = SerializeUtils.readNullableUTF(input);

        return registry
            .getDevice(deviceId)
            .map(device -> {
                PluginDeviceSession session = new PluginDeviceSession(device);
                session.connectTime = connectTime;
                session.pingTime = pingTime;
                session.timeout = timeout;
                if (pluginId != null) {
                    session.plugin = pluginLoader.apply(pluginId);
                }
                return session;
            });
    }
}
