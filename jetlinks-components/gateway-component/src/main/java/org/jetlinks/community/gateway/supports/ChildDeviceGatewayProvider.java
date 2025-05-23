/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
