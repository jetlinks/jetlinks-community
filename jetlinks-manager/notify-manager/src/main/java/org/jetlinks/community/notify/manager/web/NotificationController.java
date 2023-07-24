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
import org.jetlinks.community.notify.manager.entity.NotificationEntity;
import org.jetlinks.community.notify.manager.entity.NotifySubscriberEntity;
import org.jetlinks.community.notify.manager.enums.NotificationState;
import org.jetlinks.community.notify.manager.enums.SubscribeState;
import org.jetlinks.community.notify.manager.service.NotificationService;
import org.jetlinks.community.notify.manager.service.NotifySubscriberService;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "系统通知管理")
public class NotificationController {

    private final NotificationService notificationService;

    private final NotifySubscriberService subscriberService;

    private final List<SubscriberProvider> providers;

    public NotificationController(NotificationService notificationService,
                                  NotifySubscriberService subscriberService,
                                  List<SubscriberProvider> providers) {
        this.notificationService = notificationService;
        this.subscriberService = subscriberService;
        this.providers = providers;
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
    public Mono<NotifySubscriberEntity> doSubscribe(@RequestBody Mono<NotifySubscriberEntity> subscribe) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> subscribe
                .doOnNext(e -> {
                    e.setSubscriberType("user");
                    e.setSubscriber(auth.getUser().getId());
                })
                .flatMap(e -> subscriberService
                    .doSubscribe(e)
                    .thenReturn(e)));
    }

    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取全部订阅支持")
    public Flux<SubscriberProviderInfo> getProviders() {
        return Flux
            .fromIterable(providers)
            .map(SubscriberProviderInfo::of);
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
    @QueryOperation(summary = "按订阅类型修改通知状态")
    public Mono<Integer> readNotificationByType(@RequestBody Mono<List<String>> providerList,
                                                @PathVariable NotificationState state) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> providerList
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(list -> notificationService.createUpdate()
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

        private ConfigMetadata metadata;

        public static SubscriberProviderInfo of(SubscriberProvider provider) {
            SubscriberProviderInfo info = new SubscriberProviderInfo();
            info.id = provider.getId();
            info.name = provider.getName();
            info.setMetadata(provider.getConfigMetadata());
            return info;
        }
    }
}
