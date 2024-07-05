package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notify/channel")
@Resource(id = "notify-channel", name = "通知通道配置")
@Tag(name = "通知通道配置")
@AllArgsConstructor
public class NotifyChannelController {

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
        Flux<Tuple2<NotifySubscriberProviderEntity, List<NotifySubscriberChannelEntity>>>
            cache = infoFlux
            .map(pro -> Tuples.of(pro.toProviderEntity(), pro.toChannelEntities()))
            .cache();

        return providerService
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

        Map<String, SubscriberProviderInfo> info = SubscriberProviders
            .getProviders()
            .stream()
            .collect(Collectors.toMap(
                SubscriberProvider::getId,
                SubscriberProviderInfo::of));

        Map<String, SubscriberProviderInfo> notSaveInfoMap = new HashMap<>(info);
        return providerService
            .createQuery()
            .fetch()
            .collectList()
            .flatMap(providers -> {
                Map<String, SubscriberProviderInfo> providerInfoMap = new HashMap<>();
                for (NotifySubscriberProviderEntity provider : providers) {
                    SubscriberProviderInfo channelInfo = info.get(provider.getProvider());
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
                        .map(SubscriberProviderInfo::toProviderEntity)
                        .collect(Collectors.toList());
                    return providerService
                        .save(providerList)
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
                    SubscriberProviderInfo channelInfo = mapping.get(channel.getProviderId());
                    if (channelInfo != null) {
                        channelInfo.with(channel);
                    }
                }))
            .thenMany(Flux.fromIterable(info.values()));
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

        private Map<String, Object> configuration;

        private AuthenticationSpec grant;

        private NotifyChannelState state;

        private List<NotifySubscriberChannelEntity> channels = new ArrayList<>();

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
            info.setChannels(
                channels
                    .stream()
                    .filter(e -> e.getId() != null &&
                        (properties.isAllowAllNotify(auth) || e.getGrant() == null || e.getGrant().isGranted(auth)))
                    .collect(Collectors.toList())
            );
            return info;
        }

        public static SubscriberProviderInfo of(SubscriberProvider info) {
            return new SubscriberProviderInfo(
                IDGenerator.RANDOM.generate(),
                info.getName(),
                info.getId(),
                null,
                null,
                NotifyChannelState.disabled,
                new ArrayList<>());
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
            this.name = provider.getName();
            this.provider = provider.getProvider();
            this.grant = provider.getGrant();
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
    public static class NotifyChannelProviderInfo {
        private String id;
        private String name;

        public static NotifyChannelProviderInfo of(NotifyChannelProvider provider) {
            return new NotifyChannelProviderInfo(provider.getId(), provider.getName());
        }
    }

}
