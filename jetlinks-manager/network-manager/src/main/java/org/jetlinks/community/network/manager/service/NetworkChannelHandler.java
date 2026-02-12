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

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkProvider;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.reference.DataReferenceManager;
import org.springframework.stereotype.Component;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;

/**
 * @author liusq
 * @date 2024/3/22
 */
@Component
@AllArgsConstructor
public class NetworkChannelHandler {
    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    private final DataReferenceManager referenceManager;

    private final NetworkChannelProvider channelProvider;


    public Flux<ChannelInfo> getAliveNetworkInfo(String networkType, @Nullable String include, QueryParamEntity query) {
        NetworkProvider<?> provider = networkManager
            .getProvider(networkType)
            .orElseThrow(() -> new I18nSupportException("error.unsupported_network_type", networkType));

        return configService
            .createQuery()
            .setParam(query)
            .where(NetworkConfigEntity::getType, networkType)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .filterWhen(config -> {
                if (provider.isReusable() || Objects.equals(config.getId(), include)) {
                    return Mono.just(true);
                }
                //判断是否已经被使用
                return referenceManager
                    .isReferenced(DataReferenceManager.TYPE_NETWORK, config.getId())
                    .as(BooleanUtils::not);
            })
            .flatMap(channelProvider::toChannelInfo);
    }

    /**
     * 查询多个指定的网络组建类型的信息
     *
     * @param networkTypes 多个网络组件类型
     * @param include      包含指定的网络组件ID
     * @param query        动态查询条件
     * @return Flux<ChannelInfo>
     */
    public Flux<ChannelInfo> getAliveNetworkInfoForMoreType(Set<String> networkTypes, @Nullable String include, QueryParamEntity query) {
        if (CollectionUtils.isEmpty(networkTypes)) {
            return Flux.empty();
        }
        return configService
            .createQuery()
            .setParam(query)
            .in(NetworkConfigEntity::getType, networkTypes)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .filterWhen(config -> {
                NetworkProvider<?> provider = networkManager
                    .getProvider(config.getType())
                    .orElseThrow(() -> new I18nSupportException("error.unsupported_network_type", config.getType()));

                if (provider.isReusable() || Objects.equals(config.getId(), include)) {
                    return Mono.just(true);
                }
                //判断是否已经被使用
                return referenceManager
                    .isReferenced(DataReferenceManager.TYPE_NETWORK, config.getId())
                    .as(BooleanUtils::not);
            })
            .flatMap(channelProvider::toChannelInfo);
    }


    /**
     * 获取指定类型下全部的网络组件信息
     *
     * @param param       查询条件
     * @param networkType 网络组件类型
     */
    public Flux<ChannelInfo> getNetworkInfo(QueryParamEntity param, String networkType) {
        return configService
            .createQuery()
            .setParam(param)
            .where(NetworkConfigEntity::getType, networkType)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .flatMap(channelProvider::toChannelInfo);
    }

}
