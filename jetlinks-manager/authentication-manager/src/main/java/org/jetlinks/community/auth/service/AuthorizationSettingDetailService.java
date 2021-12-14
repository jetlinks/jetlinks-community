package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.defaults.configuration.PermissionProperties;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/**
 * 授权设置管理,用于保存授权配置以及获取授权设置详情.
 *
 * @author zhouhao
 * @see DefaultAuthorizationSettingService,DimensionProvider
 * @since 1.0
 */
@Component
@AllArgsConstructor
public class AuthorizationSettingDetailService {

    private final DefaultAuthorizationSettingService settingService;
    private final List<DimensionProvider> providers;
    private final PermissionProperties permissionProperties;

    /**
     * 保存授权设置详情
     *
     * @param menuGrantRequestMono
     * @param monoMenuEntities
     * @return
     */
    @Transactional
    public Mono<Void> saveDetail(Mono<Authentication> authenticationMono,
                                 Mono<MenuGrantRequest> menuGrantRequestMono,
                                 Mono<List<MenuEntity>> monoMenuEntities) {
        return Mono
            .zip(
                //T1: 当前用户权限信息
                authenticationMono,
                //T2: 将菜单信息转为授权信息
                Mono
                    .zip(menuGrantRequestMono,
                        monoMenuEntities,
                        MenuGrantRequest::toAuthorizationSettingDetail
                    )
                    .map(Flux::just),
                //保存授权信息
                this::saveDetail
            )
            .flatMap(Function.identity());
    }

    /**
     * 保存授权设置详情
     *
     * @param detailFlux
     * @return
     */
    @Transactional
    public Mono<Void> saveDetail(Authentication authentication, Flux<AuthorizationSettingDetail> detailFlux) {
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
                        .filter(type -> type.getId().equals(detail.getTargetType()))//过滤掉不同的维度类型
                        .singleOrEmpty()
                        .flatMap(type -> provider.getDimensionById(type, detail.getTargetId()))
                        .flatMapIterable(detail::toEntity))
                    .switchIfEmpty(Flux.defer(() -> Flux.fromIterable(detail.toEntity())))
                    .distinct(AuthorizationSettingEntity::getPermission)
            )
            .map(entity -> permissionProperties.getFilter().handleSetting(authentication, entity))
            .filter(e -> CollectionUtils.isNotEmpty(e.getActions()))
            .as(settingService::save)
            .then();
    }

    /**
     * 获取权限详情
     * @param targetType
     * @param target
     * @return
     */
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
