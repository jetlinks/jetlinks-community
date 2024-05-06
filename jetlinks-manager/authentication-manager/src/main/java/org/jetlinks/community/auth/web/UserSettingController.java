package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.UserSettingEntity;
import org.jetlinks.community.auth.service.UserSettingService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user/settings")
@Tag(name = "用户个人设置")
@AllArgsConstructor
public class UserSettingController {

    private final UserSettingService settingService;

    @GetMapping("/{type}")
    @Operation(summary = "获取指定类型的全部配置信息")
    public Flux<UserSettingEntity> getCurrentUserSettings(@PathVariable String type) {
        return Authentication
            .currentReactive()
            .flatMapMany(auth -> settingService
                .createQuery()
                .where(UserSettingEntity::getUserId, auth.getUser().getId())
                .and(UserSettingEntity::getType, type)
                .fetch());
    }

    @GetMapping("/{type}/{key}")
    @Operation(summary = "获取指定类型的单个配置信息")
    public Mono<UserSettingEntity> getCurrentUserSettings(@PathVariable String type,
                                                          @PathVariable String key) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> settingService
                .findById(UserSettingEntity.generateId(auth.getUser().getId(), type, key)));
    }

    @PostMapping("/{type}")
    @Operation(summary = "创建指定类型的配置信息")
    public Mono<Void> createSetting(@PathVariable String type,
                                    @RequestBody Mono<UserSettingEntity> body) {
        return saveSetting(type, IDGenerator.MD5.generate(), body);
    }

    @PatchMapping("/{type}/{key}")
    @Operation(summary = "保存指定类型的配置信息")
    public Mono<Void> saveSetting(@PathVariable String type,
                                  @PathVariable String key,
                                  @RequestBody Mono<UserSettingEntity> body) {
        return Mono
            .zip(
                Authentication
                    .currentReactive()
                    .switchIfEmpty(Mono.error(UnAuthorizedException::new)),
                body,
                (auth, entity) -> {
                    entity.setUserId(auth.getUser().getId());
                    entity.setType(type);
                    entity.setKey(key);
                    if(!StringUtils.hasText(entity.getName())){
                        entity.setName(entity.getKey());
                    }
                    entity.generateId();
                    return entity;
                }
            )
            .as(settingService::save)
            .then();
    }

    @DeleteMapping("/{type}/{key}")
    @Operation(summary = "删除指定类型的配置信息")
    public Mono<Void> deleteSetting(@PathVariable String type,
                                    @PathVariable String key) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(auth -> settingService
                .deleteById(UserSettingEntity.generateId(auth.getUser().getId(), type, key)))
            .then();
    }
}
