package org.jetlinks.community.configure.device;

import lombok.Generated;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionProvider;
import org.jetlinks.core.server.session.PersistentSession;
import reactor.core.publisher.Mono;

@Generated
public class UnknownDeviceSessionProvider implements DeviceSessionProvider {

    public static UnknownDeviceSessionProvider instance = new UnknownDeviceSessionProvider();

    public static UnknownDeviceSessionProvider getInstance() {
        return instance;
    }

    @Override
    public String getId() {
        return "unknown";
    }

    @Override
    public Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry) {
        return Mono.empty();
    }

    @Override
    public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
        return Mono.empty();
    }
}
