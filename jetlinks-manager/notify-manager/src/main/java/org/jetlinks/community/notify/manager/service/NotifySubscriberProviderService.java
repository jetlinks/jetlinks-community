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
package org.jetlinks.community.notify.manager.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberChannelEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberProviderEntity;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.jetlinks.community.notify.manager.web.NotifyChannelController;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NotifySubscriberProviderService
    extends GenericReactiveCacheSupportCrudService<NotifySubscriberProviderEntity, String> {

    private final ReactiveRepository<NotifySubscriberChannelEntity, String> repository;
    public Mono<Void> saveInfo(Flux<NotifyChannelController.SubscriberProviderInfo> infoFlux) {
        Flux<Tuple2<NotifySubscriberProviderEntity, List<NotifySubscriberChannelEntity>>>
            cache = infoFlux
            .map(pro -> Tuples.of(pro.toProviderEntity(), pro.toChannelEntities()))
            .cache();

        return this
            .save(cache.map(Tuple2::getT1))
            .then(repository
                      .save(cache.flatMapIterable(tp2 -> {
                          //provider保存后再回填ID
                          for (NotifySubscriberChannelEntity entity : tp2.getT2()) {
                              entity.setProviderId(tp2.getT1().getId());
                          }
                          return tp2.getT2();
                      })))
            .then();
    }


    //获取所有通道配置
    public Flux<NotifyChannelController.SubscriberProviderInfo> channels() {

        Map<String, NotifyChannelController.SubscriberProviderInfo> info = SubscriberProviders
            .getProviders()
            .stream()
            .collect(Collectors.toMap(
                SubscriberProvider::getId,
                NotifyChannelController.SubscriberProviderInfo::of));

        Map<String, NotifyChannelController.SubscriberProviderInfo> notSaveInfoMap = new HashMap<>(info);
        return createQuery()
            .fetch()
            .collectList()
            .flatMap(providers -> {
                Map<String, NotifyChannelController.SubscriberProviderInfo> providerInfoMap = new HashMap<>();
                for (NotifySubscriberProviderEntity provider : providers) {
                    NotifyChannelController.SubscriberProviderInfo channelInfo = info.get(provider.getProvider());
                    if (channelInfo != null) {
                        channelInfo.with(provider);
                        providerInfoMap.put(channelInfo.getId(), channelInfo);
                    }
                    if (info.get(provider.getProvider()) != null) {
                        notSaveInfoMap.remove(provider.getProvider());
                    }
                }
                if (!MapUtils.isEmpty(notSaveInfoMap)) {
                    List<NotifySubscriberProviderEntity> providerList = notSaveInfoMap
                        .values()
                        .stream()
                        .map(NotifyChannelController.SubscriberProviderInfo::toProviderEntity)
                        .collect(Collectors.toList());
                    return save(providerList)
                        .thenReturn(providerInfoMap);
                }
                return Mono.just(providerInfoMap);
            })
            .filter(MapUtils::isNotEmpty)
            .flatMapMany(mapping -> repository
                .createQuery()
                .in(NotifySubscriberChannelEntity::getProviderId, mapping.keySet())
                .fetch()
                .doOnNext(channel -> {
                    NotifyChannelController.SubscriberProviderInfo channelInfo = mapping.get(channel.getProviderId());
                    if (channelInfo != null) {
                        channelInfo.with(channel);
                    }
                }))
            .thenMany(Flux.fromIterable(info.values()))
            .sort(Comparator.comparing(NotifyChannelController.SubscriberProviderInfo::getOrder));
    }
}
