package org.jetlinks.community.auth.web;

import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/autz-setting/detail")
@Authorize
@Resource(
    id = "autz-setting",
    name = "权限分配",
    group = {"system"}
)
public class AuthorizationSettingDetailController {

    private final DefaultAuthorizationSettingService settingService;

    private final List<DimensionProvider> providers;

    public AuthorizationSettingDetailController(DefaultAuthorizationSettingService settingService, List<DimensionProvider> providers) {
        this.settingService = settingService;
        this.providers = providers;
    }

    @PostMapping("/_save")
    @SaveAction
    public Mono<Boolean> saveSettings(@RequestBody Flux<AuthorizationSettingDetail> detailFlux) {
        return detailFlux
            //先删除旧的权限设置
            .flatMap(detail -> settingService.getRepository().createDelete()
                .where(AuthorizationSettingEntity::getDimensionType, detail.getTargetType())
                .and(AuthorizationSettingEntity::getDimensionTarget, detail.getTargetId())
                .execute()
                .thenReturn(detail))
            .flatMap(detail ->
                Flux.fromIterable(providers)
                    .flatMap(provider -> provider
                        .getAllType()
                        .filter(type -> type.getId().equals(detail.getTargetType()))
                        .singleOrEmpty()
                        .flatMap(type -> provider.getDimensionById(type, detail.getTargetId())))
                    .singleOrEmpty()
                    .flatMapIterable(detail::toEntity)
                    .switchIfEmpty(Flux.defer(() -> Flux.fromIterable(detail.toEntity())))
            )
            .as(settingService::save)
            .thenReturn(true);
    }

    @GetMapping("/{targetType}/{target}")
    @SaveAction
    public Mono<AuthorizationSettingDetail> getSettings(@PathVariable String targetType, @PathVariable String target) {


        return settingService
            .createQuery()
            .where(AuthorizationSettingEntity::getDimensionTarget, target)
            .and(AuthorizationSettingEntity::getDimensionType, targetType)
            .fetch()
            .collectList()
            .map(AuthorizationSettingDetail::fromEntity)
            ;
    }

}
