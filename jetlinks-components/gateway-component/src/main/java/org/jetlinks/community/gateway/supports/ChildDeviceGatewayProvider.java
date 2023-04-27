package org.jetlinks.community.gateway.supports;

import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.AbstractDeviceGateway;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.NetworkType;
import reactor.core.publisher.Mono;

public class ChildDeviceGatewayProvider implements DeviceGatewayProvider {

    @Override
    public String getId() {
        return "child-device";
    }

    @Override
    public String getName() {
        return "网关子设备接入";
    }

    @Override
    public String getDescription() {
        return "通过其他网关设备来接入子设备";
    }

    @Override
    public String getChannel() {
        return "child-device";
    }


    public Transport getTransport() {
        return Transport.of("Gateway");
    }

    @Override
    public Mono<? extends DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return Mono.just(new ChildDeviceGateway(properties.getId()));
    }

    static class ChildDeviceGateway extends AbstractDeviceGateway {

        public ChildDeviceGateway(String id) {
            super(id);
        }

        @Override
        protected Mono<Void> doShutdown() {
            return Mono.empty();
        }

        @Override
        protected Mono<Void> doStartup() {
            return Mono.empty();
        }
    }
}
