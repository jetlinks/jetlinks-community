package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.jetlinks.community.auth.service.ThirdPartyUserBindService;
import org.jetlinks.community.auth.web.request.ThirdPartyBindUserInfo;
import org.jetlinks.community.service.UserBindService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user/third-party")
@AllArgsConstructor
@Resource(id = "user-third-party-manager", name = "第三方用户")
@Tag(name = "第三方用户")
public class ThirdPartyUserController {


    private final ThirdPartyUserBindService thirdPartyUserBindService;

    private final ReactiveRepository<ThirdPartyUserBindEntity, String> repository;

    private final UserBindService userBindService;

    @PatchMapping("/{type}/{provider}")
    @Operation(summary = "绑定用户")
    @SaveAction
    @Deprecated
    public Mono<Void> bindUser(@PathVariable String type,
                               @PathVariable String provider,
                               @RequestBody(required = false) Flux<ThirdPartyBindUserInfo> requestFlux) {
        return bind(type, provider, requestFlux);
    }

    @PostMapping("/{type}/{provider}/_bind")
    @Operation(summary = "绑定用户")
    @SaveAction
    public Mono<Void> bind(@PathVariable String type,
                           @PathVariable String provider,
                           @RequestBody(required = false) Flux<ThirdPartyBindUserInfo> requestFlux) {

        return requestFlux
            .map(request -> {
                ThirdPartyUserBindEntity entity = new ThirdPartyUserBindEntity();
                entity.setType(type);
                entity.setProvider(provider);
                entity.setThirdPartyUserId(request.getThirdPartyUserId());
                entity.setUserId(request.getUserId());
                entity.setProviderName(request.getProviderName());
                entity.generateId();
                return entity;
            })
            .as(repository::save)
            .then();
    }


    @PostMapping("/{id}/_unbind")
    @Operation(summary = "解绑用户")
    @SaveAction
    public Mono<Void> unbind(@PathVariable String id) {

        return repository
            .deleteById(id)
            .then();
    }

    @PostMapping("/me/{type}/{provider}/{bindCode}/_bind")
    @Operation(summary = "根据绑定码绑定当前用户")
    @Authorize(merge = false)
    public Mono<Void> bindByCode(@PathVariable String type,
                                 @PathVariable String provider,
                                 @PathVariable String bindCode) {

        return Authentication
            .currentReactive()
            .flatMap(authentication -> userBindService
                .getUserInfoByCode(bindCode)
                .doOnNext(userInfo -> userBindService.checkUserBind(authentication, userInfo))
                .map(userInfo -> {
                    ThirdPartyUserBindEntity entity = new ThirdPartyUserBindEntity();
                    entity.setType(type);
                    entity.setProvider(provider);
                    entity.setThirdPartyUserId(userInfo.getThirdPartyUserId());
                    entity.setUserId(authentication.getUser().getId());
                    entity.setProviderName(userInfo.getName());
                    entity.generateId();
                    return entity;
                }))
            .as(thirdPartyUserBindService::save)
            .then();
    }


    @GetMapping("/{type}/{provider}")
    @Operation(summary = "获取绑定信息")
    @QueryAction
    public Flux<ThirdPartyBindUserInfo> queryBindings(@PathVariable String type,
                                                      @PathVariable String provider) {

        return repository
            .createQuery()
            .where(ThirdPartyUserBindEntity::getType, type)
            .and(ThirdPartyUserBindEntity::getProvider, provider)
            .fetch()
            .map(bind -> ThirdPartyBindUserInfo.of(
                bind.getId(), bind.getUserId(), bind.getProviderName(), bind.getThirdPartyUserId()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户绑定信息")
    @Authorize(merge = false)
    public Flux<ThirdPartyUserBindEntity> getCurrentUserBindings() {
        return Authentication
            .currentReactive()
            .flatMapMany(auth -> repository
                .createQuery()
                .where(ThirdPartyUserBindEntity::getUserId, auth.getUser().getId())
                .fetch());
    }

    @DeleteMapping("/me/{bindingId}")
    @Operation(summary = "解绑当前用户绑定信息")
    @Authorize(merge = false)
    public Mono<Void> deleteBinding(@PathVariable String bindingId) {
        return Authentication
            .currentReactive()
            .flatMap(auth -> repository
                .createDelete()
                .where(ThirdPartyUserBindEntity::getUserId, auth.getUser().getId())
                .and(ThirdPartyUserBindEntity::getId, bindingId)
                .execute()
            )
            .then();
    }
}
