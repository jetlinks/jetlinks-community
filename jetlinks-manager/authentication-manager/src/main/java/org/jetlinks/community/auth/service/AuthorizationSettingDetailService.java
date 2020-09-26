package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@AllArgsConstructor
public class AuthorizationSettingDetailService {

    private final DefaultAuthorizationSettingService settingService;
    private final List<DimensionProvider> providers;

    @Transactional
    public Mono<Void> saveDetail(Flux<AuthorizationSettingDetail> detailFlux) {
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
                        .flatMap(type -> provider.getDimensionById(type, detail.getTargetId()))
                        .flatMapIterable(detail::toEntity))
                    .switchIfEmpty(Flux.defer(() -> Flux.fromIterable(detail.toEntity())))
                    .distinct(AuthorizationSettingEntity::getPermission)
            )
            .as(settingService::save)
            .then();
    }

    public Mono<AuthorizationSettingDetail> getSettingDetail(String targetType,
                                                             String target) {
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
