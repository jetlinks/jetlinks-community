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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProviders;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.manager.configuration.NotifySubscriberProperties;
import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.service.NotificationService;
import org.jetlinks.community.notify.manager.service.NotifySubscriberProviderService;
import org.jetlinks.community.notify.manager.service.NotifySubscriberService;
import org.jetlinks.community.notify.subscription.SubscribeType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "系统通知管理")
public class NotificationController {

    private final NotificationService notificationService;

    private final NotifySubscriberService subscriberService;

    private final NotifySubscriberProviderService providerService;

    private final NotifySubscriberProperties properties;

    public NotificationController(NotificationService notificationService,
                                  NotifySubscriberService subscriberService,
                                  NotifySubscriberProviderService providerService,
                                  NotifySubscriberProperties properties) {
        this.notificationService = notificationService;
        this.subscriberService = subscriberService;
        this.providerService = providerService;
        this.properties = properties;
    }

    @GetMapping("/subscriptions/_query")
    @Authorize(ignore = true)
    @QueryOperation(summary = "查询当前用户订阅信息")
    public Mono<PagerResult<NotifySubscriberEntity>> querySubscription(@Parameter(hidden = true) QueryParamEntity query) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> query
                .toNestQuery(q -> q
                    .where(NotifySubscriberEntity::getSubscriberType, "user")
                    .and(NotifySubscriberEntity::getSubscriber, auth.getUser().getId()))
                .execute(subscriberService::queryPager));

    }

    @PostMapping("/subscriptions/_query")
    @Authorize(ignore = true)
    @Operation(summary = "POST查询当前用户订阅信息")
    public Mono<PagerResult<NotifySubscriberEntity>> querySubscription(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(this::querySubscription);
    }

    @PutMapping("/subscription/{id}/_{state}")
    @Authorize(ignore = true)
    @Operation(summary = "修改通知订阅状态")
    public Mono<Void> changeSubscribeState(@PathVariable String id, @PathVariable SubscribeState state) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> subscriberService
                .createUpdate()
                .set(NotifySubscriberEntity::getState, state)
                .where(NotifySubscriberEntity::getId, id)
                .and(NotifySubscriberEntity::getSubscriber, auth.getUser().getId())
                .and(NotifySubscriberEntity::getSubscriberType, "user")
                .execute()
                .then()
            );
    }

    @DeleteMapping("/subscription/{id}")
    @Authorize(ignore = true)
    @Operation(summary = "删除订阅")
    public Mono<Void> deleteSubscription(@PathVariable String id) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> subscriberService
                .createDelete()
                .where(NotifySubscriberEntity::getId, id)
                .and(NotifySubscriberEntity::getSubscriber, auth.getUser().getId())
                .and(NotifySubscriberEntity::getSubscriberType, "user")
                .execute()
                .then()
            );
    }

    @PatchMapping("/subscribe")
    @Authorize(ignore = true)
    @Operation(summary = "订阅通知")
    public Flux<NotifySubscriberEntity> doSubscribe(@RequestBody Flux<NotifySubscriberEntity> subscribe) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> subscribe
                .doOnNext(e -> {
                    e.setSubscriberType("user");
                    e.setSubscriber(auth.getUser().getId());
                })
                .flatMap(e -> subscriberService
                    .doSubscribe(e)
                    .thenReturn(e)));
    }

    /**
     * @see NotifyChannelController#getChannelProviders()
     * @deprecated
     */
    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取全部订阅支持")
    public Flux<SubscriberProviderInfo> getProviders() {
        return Flux
            .fromIterable(SubscriberProviders.getProviders())
            .map(SubscriberProviderInfo::of);
    }

    /**
     * @see NotifyChannelController#getChannelProviders()
     * @deprecated
     */
    @GetMapping("/current/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可用的订阅支持")
    public Flux<SubscriberProviderInfo> getCurrentProviders() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> providerService
                .channels()
                .mapNotNull(info -> info.copyToProvidedUser(auth, properties)))
            .filter(p -> CollectionUtils.isNotEmpty(p.getChannels()))
            .map(NotifyChannelController.SubscriberProviderInfo::getProvider)
            .collectList()
            .flatMapMany(providers -> Flux
                .fromIterable(SubscriberProviders.getProviders())
                .filter(provider -> providers.contains(provider.getId()))
                .map(SubscriberProviderInfo::of));
    }

    /**
     * @see NotifyChannelController#getChannelProviders()
     * @deprecated
     */
    @GetMapping("/current/{type}/providers")
    @Authorize(merge = false)
    @Operation(summary = "根据订阅类型获取当前用户可用的订阅支持")
    public Flux<SubscriberProviderInfo> getCurrentProviders(@PathVariable String type) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> providerService
                .channels()
                .mapNotNull(info -> info.copyToProvidedUser(auth, properties)))
            .filter(p -> CollectionUtils.isNotEmpty(p.getChannels()))
            .map(NotifyChannelController.SubscriberProviderInfo::getProvider)
            .collectList()
            .flatMapMany(providers -> Flux
                .fromIterable(SubscriberProviders.getProviders())
                .filter(provider -> provider.getType().getId().equals(type) && providers.contains(provider.getId()))
                .sort(Comparator.comparing(SubscriberProvider::getOrder))
                .map(SubscriberProviderInfo::of));
    }

    @GetMapping("/_query")
    @Authorize(ignore = true)
    @QueryOperation(summary = "查询通知记录")
    public Mono<PagerResult<NotificationEntity>> queryMyNotifications(@Parameter(hidden = true) QueryParamEntity query) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> query
                .toNestQuery(q -> q
                    .where(NotificationEntity::getSubscriberType, "user")
                    .and(NotificationEntity::getSubscriber, auth.getUser().getId()))
                .execute(notificationService::queryPager)
                .defaultIfEmpty(PagerResult.empty()));

    }

    @PostMapping("/_query")
    @Authorize(ignore = true)
    @Operation(summary = "使用POST方式查询通知记录")
    public Mono<PagerResult<NotificationEntity>> queryMyNotifications(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(this::queryMyNotifications);
    }

    @GetMapping("/{id}/read")
    @Authorize(ignore = true)
    @QueryOperation(summary = "获取通知记录")
    public Mono<NotificationEntity> readNotification(@PathVariable String id) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> QueryParamEntity
                .newQuery()
                .where(NotificationEntity::getSubscriberType, "user")
                .and(NotificationEntity::getSubscriber, auth.getUser().getId())
                .and(NotificationEntity::getId, id)
                .execute(notificationService::findAndMarkRead)
                .singleOrEmpty()
            );
    }

    @PostMapping("/_{state}")
    @Authorize(ignore = true)
    @QueryOperation(summary = "修改通知状态")
    public Mono<Integer> readNotification(@RequestBody Mono<List<String>> idList,
                                          @PathVariable NotificationState state) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> idList
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(list -> notificationService
                    .createUpdate()
                    .set(NotificationEntity::getState, state)
                    .where(NotificationEntity::getSubscriberType, "user")
                    .and(NotificationEntity::getSubscriber, auth.getUser().getId())
                    .in(NotificationEntity::getId, list)
                    .execute())
            );
    }

    @PostMapping("/_{state}/provider")
    @Authorize(ignore = true)
    @QueryOperation(summary = "按订阅具体类型修改通知状态")
    public Mono<Integer> readNotificationByType(@RequestBody Mono<List<String>> providerList,
                                                @PathVariable NotificationState state) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> providerList
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(list -> notificationService
                    .createUpdate()
                    .set(NotificationEntity::getState, state)
                    .where(NotificationEntity::getSubscriberType, "user")
                    .and(NotificationEntity::getSubscriber, auth.getUser().getId())
                    .in(NotificationEntity::getTopicProvider, list)
                    .execute())
            );
    }

    @Getter
    @Setter
    public static class SubscriberProviderInfo {
        private String id;

        private String name;

        private SubscribeTypeInfo type;

        private ConfigMetadata metadata;

        public String getName() {
            return LocaleUtils.resolveMessage("message.subscriber.provider." + id, name);
        }

        public static SubscriberProviderInfo of(SubscriberProvider provider) {
            SubscriberProviderInfo info = new SubscriberProviderInfo();
            info.id = provider.getId();
            info.name = provider.getName();
            info.type = SubscribeTypeInfo.of(provider.getType());
            info.setMetadata(provider.getConfigMetadata());
            return info;
        }
    }

    @Getter
    @Setter
    public static class SubscribeTypeInfo {

        private String id;

        private String name;

        public static SubscribeTypeInfo of(SubscribeType type) {
            SubscribeTypeInfo info = new SubscribeTypeInfo();
            info.id = type.getId();
            info.name = type.getName();
            return info;
        }
    }

}
