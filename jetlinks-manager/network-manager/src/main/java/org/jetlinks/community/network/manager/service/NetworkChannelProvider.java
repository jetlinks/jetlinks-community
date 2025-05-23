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
package org.jetlinks.community.network.manager.service;

import lombok.AllArgsConstructor;
import org.jetlinks.community.network.ClientNetworkConfig;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.ServerNetworkConfig;
import org.jetlinks.community.network.channel.Address;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.channel.ChannelProvider;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class NetworkChannelProvider implements ChannelProvider {

    public static final String CHANNEL = "network";

    private final NetworkManager networkManager;

    private final NetworkConfigService configService;

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public Mono<ChannelInfo> getChannelInfo(String channelId) {
        return configService
            .findById(channelId)
            .flatMap(this::toChannelInfo);
    }

    public Mono<ChannelInfo> toChannelInfo(NetworkConfigEntity entity) {
        ChannelInfo info = new ChannelInfo();
        info.setId(entity.getId());
        info.setDescription(entity.getDescription());
        info.setName(entity.getName());
        return Mono
            .justOrEmpty(networkManager.getProvider(entity.getType()))
            .flatMap(provider -> Flux
                .fromIterable(entity.toNetworkPropertiesList())
                .flatMap(provider::createConfig)
                .mapNotNull(conf -> {
                    if (conf instanceof ClientNetworkConfig) {
                        //客户端则返回远程地址
                        return ((ClientNetworkConfig) conf).getRemoteAddress();
                    }
                    if (conf instanceof ServerNetworkConfig) {
                        //服务端返回公共访问地址
                        return ((ServerNetworkConfig) conf).getPublicAddress();
                    }
                    return null;
                })
                .distinct()
                .map(address -> Address.of(address,
                                           //todo 真实状态检查?
                                           entity.getState() == NetworkConfigState.enabled
                                               ? Address.HEALTH_OK
                                               : Address.HEALTH_DISABLED))
                .collectList())
            .doOnNext(info::setAddresses)
            .thenReturn(info);

    }
}
