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
package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nullable;
import java.util.*;
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
            .<Set<String>>collect(HashSet::new, Set::add)
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
        MultiValueMap<String,String> delDimension=new LinkedMultiValueMap<>();
        return detailFlux
                .doOnNext(detail->delDimension.add(detail.getTargetType(),detail.getTargetId()))
                .then(Mono.defer(()->{
                    //先删除旧的权限设置
                   return Flux
                            .fromIterable(delDimension.entrySet())
                            .filter(entry->CollectionUtils.isNotEmpty(entry.getValue()))
                            .flatMap(entry-> settingService
                                    .getRepository()
                                    .createDelete()
                                    .where(AuthorizationSettingEntity::getDimensionType, entry.getKey())
                                    .in(AuthorizationSettingEntity::getDimensionTarget,entry.getValue())
                                    .execute())
                            .then();

                }))
                .then(addDetail(currentAuthentication, detailFlux));
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
            )
            .as(this::mergePermissionActionAndDataAccesses)
            .map(entity -> null == currentAuthentication
                ? entity
                : permissionProperties.getFilter().handleSetting(currentAuthentication, entity))
            .filter(e -> CollectionUtils.isNotEmpty(e.getActions()))
            .as(settingService::save)
            .then();
    }


    public Flux<AuthorizationSettingEntity> mergePermissionActionAndDataAccesses(Flux<AuthorizationSettingEntity> entityFlux) {
        return entityFlux
                .doOnNext(AuthorizationSettingDetailService::generateId)
                .groupBy(AuthorizationSettingEntity::getId)
                .flatMap(entityGroup -> {
                    AuthorizationSettingEntity newOne = new AuthorizationSettingEntity();
                    return entityGroup
                            .switchOnFirst((signal, entities) -> {
                                if (signal.hasValue() && signal.get() != null) {
                                    signal.get().copyTo(newOne);
                                    newOne.setDataAccesses(new ArrayList<>());
                                }
                                return entities;
                            })
                            //根据权重排序
                            .sort(Comparator.comparingInt(AuthorizationSettingEntity::getPriority))
                            .doOnNext(entity -> {
                                boolean merge = Boolean.TRUE.equals(entity.getMerge());
                                newOne.setPriority(Math.max(newOne.getPriority(),entity.getPriority()));
                                if (!merge) {
                                    newOne.getActions().clear();
                                }

                                if (CollectionUtils.isNotEmpty(entity.getDataAccesses())) {
                                    newOne.getDataAccesses().addAll(entity.getDataAccesses());
                                }
                                if (CollectionUtils.isNotEmpty(entity.getActions())) {
                                    newOne.getActions().addAll(entity.getActions());
                                }
                            })
                            .then(Mono.just(newOne));
                });
    }

    public static void generateId(AuthorizationSettingEntity entity) {
        if (StringUtils.isEmpty(entity.getId())) {
            entity.setId(DigestUtils.md5Hex(entity.getPermission() + entity.getDimensionType() + entity.getDimensionTarget()));
        }
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
