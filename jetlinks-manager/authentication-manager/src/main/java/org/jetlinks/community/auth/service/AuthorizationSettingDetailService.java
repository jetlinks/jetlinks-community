package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.defaults.configuration.PermissionProperties;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权设置管理,用于保存授权配置以及获取授权设置详情.
 *
 * @author zhouhao
 * @see DefaultAuthorizationSettingService
 * @see DimensionProvider
 * @since 1.0
 */
@Component
@AllArgsConstructor
public class AuthorizationSettingDetailService {

    private final DefaultAuthorizationSettingService settingService;
    private final List<DimensionProvider> providers;
    private final PermissionProperties permissionProperties;

    private final ApplicationEventPublisher eventPublisher;

    public Mono<Void> clearPermissionUserAuth(String type,String id){
        return Flux
            .fromIterable(providers)
            .flatMap(provider ->
                         //按维度类型进行映射
                         provider.getAllType()
                                 .map(DimensionType::getId)
                                 .map(t -> Tuples.of(t, provider)))
            .collectMap(Tuple2::getT1, Tuple2::getT2)
            .flatMapMany(typeProviderMapping -> Mono
                .justOrEmpty(typeProviderMapping.get(type))
                .flatMapMany(provider -> provider.getUserIdByDimensionId(id)))
            .collectList()
            .flatMap(lst-> ClearUserAuthorizationCacheEvent.of(lst).publish(eventPublisher))
            .then();
    }

    /**
     * 保存授权设置详情，此操作会全量覆盖数据
     *
     * @param currentAuthentication 当前用户权限信息
     * @param detailFlux            授权详情
     * @return void
     */
    @Transactional
    public Mono<Void> saveDetail(@Nullable Authentication currentAuthentication,
                                 Flux<AuthorizationSettingDetail> detailFlux) {
        return detailFlux
            //先删除旧的权限设置
            .flatMap(detail -> settingService
                .getRepository()
                .createDelete()
                .where(AuthorizationSettingEntity::getDimensionType, detail.getTargetType())
                .and(AuthorizationSettingEntity::getDimensionTarget, detail.getTargetId())
                .execute()
                .thenReturn(detail))
            .flatMap(detail -> addDetail(currentAuthentication, detailFlux))
            .then();
    }


    /**
     * 增量添加授权设置详情
     *
     * @param currentAuthentication 当前用户权限信息
     * @param detailFlux            授权详情
     * @return void
     */
    @Transactional
    public Mono<Void> addDetail(@Nullable Authentication currentAuthentication,
                                   Flux<AuthorizationSettingDetail> detailFlux) {
        return detailFlux
            .flatMap(detail -> Flux
                .fromIterable(providers)
                .flatMap(provider -> provider
                    .getAllType()
                    .filter(type -> type.getId().equals(detail.getTargetType()))//过滤掉不同的维度类型
                    .singleOrEmpty()
                    .flatMap(type -> provider.getDimensionById(type, detail.getTargetId()))
                    .flatMapIterable(detail::toEntity))
                .switchIfEmpty(Flux.defer(() -> Flux.fromIterable(detail.toEntity())))
                .distinct(AuthorizationSettingEntity::getPermission)
            )
            .map(entity -> null == currentAuthentication
                ? entity
                : permissionProperties.getFilter().handleSetting(currentAuthentication, entity))
            .filter(e -> CollectionUtils.isNotEmpty(e.getActions()))
            .as(settingService::save)
            .then();
    }

    /**
     * 删除授权设置详情
     *
     * @param detailFlux   授权详情
     * @return void
     */
    @Transactional
    public Mono<Void> deleteDetail(Flux<AuthorizationSettingDetail> detailFlux) {
        return detailFlux
            .flatMap(detail -> settingService
                .getRepository()
                .createDelete()
                .where(AuthorizationSettingEntity::getDimensionType, detail.getTargetType())
                .and(AuthorizationSettingEntity::getDimensionTarget, detail.getTargetId())
                .in(AuthorizationSettingEntity::getPermission,
                    detail
                        .getPermissionList()
                        .stream()
                        .map(AuthorizationSettingDetail.PermissionInfo::getId)
                        .collect(Collectors.toList()))
                .execute())
            .then();

    }

    //获取权限详情
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
