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
package org.jetlinks.community.notify.manager.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.authorize.AuthenticationSpec;
import org.jetlinks.community.notify.manager.configuration.NotifySubscriberProperties;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberChannelEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberProviderEntity;
import org.jetlinks.community.notify.manager.enums.NotifyChannelState;
import org.jetlinks.community.notify.manager.service.NotifySubscriberProviderService;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.jetlinks.community.notify.manager.subscriber.channel.NotifyChannelProvider;
import org.jetlinks.community.notify.subscription.SubscribeType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notify/channel")
@Resource(id = "notify-channel", name = "通知通道配置")
@Tag(name = "通知通道配置")
@AllArgsConstructor
public class NotifyChannelController {

    @SuppressWarnings("all")
    private final ReactiveRepository<NotifySubscriberChannelEntity, String> repository;

    private final NotifySubscriberProviderService providerService;

    private final List<NotifyChannelProvider> channelProviders;

    private final NotifySubscriberProperties properties;

    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取通知通道提供商信息")
    public Flux<NotifyChannelProviderInfo> getChannelProviders() {
        return Flux
            .fromIterable(channelProviders)
            .map(NotifyChannelProviderInfo::of);
    }


    @PatchMapping
    @SaveAction
    @Operation(summary = "保存通道配置")
    public Mono<Void> save(@RequestBody Flux<SubscriberProviderInfo> infoFlux) {
        return providerService.saveInfo(infoFlux);
    }

    @PatchMapping("/{providerId}")
    @SaveAction
    @Operation(summary = "保存单个渠道的通道配置")
    public Mono<Void> saveChannel(@PathVariable String providerId,
                                  @RequestBody Flux<NotifySubscriberChannelEntity> channels) {
        return repository
            .save(channels.doOnNext(c -> c.setProviderId(providerId)))
            .then();
    }

    @DeleteMapping("/{channelId}")
    @DeleteAction
    @Operation(summary = "删除通道")
    public Mono<Void> saveChannel(@PathVariable String channelId) {
        return repository
            .deleteById(channelId)
            .then();
    }

    @PostMapping("/{providerId}/enable")
    @Operation(summary = "启用订阅")
    @SaveAction
    public Mono<Void> enableProvider(@PathVariable String providerId) {
        return providerService
            .findById(providerId)
            .flatMap(provider -> {
                provider.setState(NotifyChannelState.enabled);
                return providerService.updateById(providerId, provider);
            })
            .then();
    }

    @PostMapping("/{providerId}/disable")
    @Operation(summary = "禁用订阅")
    @SaveAction
    public Mono<Void> disableProvider(@PathVariable String providerId) {
        return providerService
            .findById(providerId)
            .flatMap(provider -> {
                provider.setState(NotifyChannelState.disabled);
                return providerService.updateById(providerId, provider);
            })
            .then();
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "修改订阅")
    @SaveAction
    public Mono<Void> updateProvider(@PathVariable String providerId,
                                     @RequestBody Mono<NotifySubscriberProviderEntity> provider) {
        return providerService
            .updateById(providerId, provider)
            .then();
    }

    @GetMapping("/all-for-save")
    @SaveAction //有保存权限的才能查看全部
    @Operation(summary = "获取所有通道配置")
    public Flux<SubscriberProviderInfo> channels() {
        return providerService.channels();
    }

    @GetMapping("/all")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的通道配置")
    public Flux<SubscriberProviderInfo> accessibleChannels() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> channels().mapNotNull(info -> info.copyToProvidedUser(auth, properties)))
            .filter(p -> CollectionUtils.isNotEmpty(p.getChannels()));
    }

    @GetMapping("/{providerId}/variables")
    @Authorize(merge = false)
    @Operation(summary = "获取通知的内置参数")
    public Flux<PropertyMetadata> notifyVariables(@PathVariable String providerId) {
        return providerService
            .findById(providerId)
            .flatMapMany(entity -> Mono
                .justOrEmpty(SubscriberProviders.getProvider(entity.getProvider()))
                .flatMapMany(provider -> provider.getDetailProperties(entity.getConfiguration())));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscriberProviderInfo {

        private String id;

        private String name;

        private String provider;

        private SubscribeTypeInfo type;

        private Map<String, Object> configuration;

        private AuthenticationSpec grant;

        private NotifyChannelState state;

        private List<NotifySubscriberChannelEntity> channels = new ArrayList<>();

        private int order;

        public SubscriberProviderInfo(String id,
                                      String name,
                                      String provider,
                                      SubscribeTypeInfo type,
                                      Map<String, Object> configuration,
                                      AuthenticationSpec grant,
                                      NotifyChannelState state,
                                      List<NotifySubscriberChannelEntity> channels) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.type = type;
            this.configuration = configuration;
            this.grant = grant;
            this.state = state;
            this.channels = channels;
        }

        public SubscriberProviderInfo copyToProvidedUser(Authentication auth, NotifySubscriberProperties properties) {
            if (id == null
                || (state == null || state == NotifyChannelState.disabled)
                || (!properties.isAllowAllNotify(auth) && grant != null && !grant.isGranted(auth))) {
                return null;
            }

            SubscriberProviderInfo info = new SubscriberProviderInfo();
            info.setId(id);
            info.setName(name);
            info.setProvider(provider);
            info.setType(type);
            info.setChannels(
                channels
                    .stream()
                    .filter(channel -> (
                        (channel.getId() != null)
                            && (
                            properties.isAllowAllNotify(auth)
                                || (channel.getGrant() != null && channel.getGrant().isGranted(auth))
                        )
                    ))
                    .collect(Collectors.toList())
            );
            return info;
        }

        public static SubscriberProviderInfo of(SubscriberProvider info) {
            return new SubscriberProviderInfo(
                IDGenerator.RANDOM.generate(),
                info.getName(),
                info.getId(),
                SubscribeTypeInfo.of(info.getType()),
                null,
                null,
                NotifyChannelState.disabled,
                new ArrayList<>(),
                info.getOrder()
            );
        }

        public SubscriberProviderInfo with(List<NotifyChannelProvider> provider) {

            for (NotifyChannelProvider notifyChannelProvider : provider) {
                NotifySubscriberChannelEntity entity = new NotifySubscriberChannelEntity();
                entity.setChannelProvider(notifyChannelProvider.getId());
                entity.setName(notifyChannelProvider.getName());
                channels.add(entity);
            }
            return this;
        }

        public SubscriberProviderInfo with(NotifySubscriberChannelEntity channel) {
            boolean matched = false;
            for (NotifySubscriberChannelEntity entity : channels) {
                if (Objects.equals(entity.getChannelProvider(), channel.getChannelProvider())
                    && entity.getId() == null) {
                    matched = true;
                    channel.copyTo(entity);
                }
            }
            if (!matched) {
                channels.add(channel);
            }
            return this;
        }

        public SubscriberProviderInfo with(NotifySubscriberProviderEntity provider) {
            this.id = provider.getId();
            this.grant = provider.getGrant();
            this.provider = provider.getProvider();
            this.configuration = provider.getConfiguration();
            this.state = provider.getState();
            return this;
        }

        public NotifySubscriberProviderEntity toProviderEntity() {

            return new NotifySubscriberProviderEntity().copyFrom(this);
        }

        public List<NotifySubscriberChannelEntity> toChannelEntities() {
            if (CollectionUtils.isEmpty(channels)) {
                return Collections.emptyList();
            }
            for (NotifySubscriberChannelEntity channel : channels) {
                channel.setProviderId(this.id);
            }
            return channels;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscribeTypeInfo {
        private String id;
        @JsonIgnore
        SubscribeType type;

        public static SubscribeTypeInfo of(SubscribeType type) {
            return new SubscribeTypeInfo(type.getId(), type);
        }

        public String getName() {
            return type.getName();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotifyChannelProviderInfo {
        private String id;
        private String name;

        public static NotifyChannelProviderInfo of(NotifyChannelProvider provider) {
            return new NotifyChannelProviderInfo(provider.getId(), provider.getName());
        }
    }

}
