package org.jetlinks.community.auth.service;

import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuGrantEntity;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单授权管理
 *
 * @author zeje
 */
@Service
public class MenuGrantService extends GenericReactiveCrudService<MenuGrantEntity, String> {

    @Transactional(rollbackFor = Exception.class)
    public Mono<SaveResult> save(Mono<MenuGrantRequest> menuGrantRequestMono,
                                 Mono<List<MenuEntity>> cacheMonoMenuEntities) {

        final Flux<MenuGrantEntity> menuGrantEntityFlux =
            Flux.zip(menuGrantRequestMono, cacheMonoMenuEntities)
                .flatMap(it -> {
                    final MenuGrantRequest menuGrantRequest = it.getT1();
                    return Flux.fromIterable(menuGrantRequest.toMenuGrantEntities(it.getT2()));
                });

        final Flux<MenuGrantEntity> menuGrantEntityFluxPlus =
            menuGrantRequestMono.flatMap(it -> {
                    //先删除旧的菜单授权
                    ReactiveDelete reactiveDelete = this.getRepository().createDelete()
                        .where(MenuGrantEntity::getDimensionTypeId, it.getTargetType())
                        .and(MenuGrantEntity::getDimensionId, it.getTargetId());
                    //若有指定作用域，则只处理对应作用域的菜单授权
                    if (it.getMenuScope() != null) {
                        reactiveDelete = reactiveDelete.and(MenuGrantEntity::getMenuScope, it.getMenuScope());
                    }
                    return reactiveDelete.execute();
                }
            ).thenMany(menuGrantEntityFlux);

        return save(menuGrantEntityFluxPlus);
    }

    /**
     * 获取菜单授权
     *
     * @param userId
     * @param dimensions
     * @return
     */
    public Mono<Map<String, Collection<MenuGrantEntity>>> getMenuGrantMap(String userId, List<Dimension> dimensions) {

        //获取用户的菜单授权
        ReactiveQuery<MenuGrantEntity> query = this
            .createQuery()
            .where(MenuGrantEntity::getDimensionTypeId, DefaultDimensionType.user.toString())
            .and(MenuGrantEntity::getDimensionId, userId);

        //获取维度下对应的菜单授权（应该只有角色）
        if (CollectionUtils.isNotEmpty(dimensions)) {
            //分组
            final Map<DimensionType, List<Dimension>> map = dimensions.stream()
                .collect(Collectors.groupingBy(Dimension::getType));
            //遍历分组，组装表达式
            for (Map.Entry<DimensionType, List<Dimension>> entry : map.entrySet()) {
                final List<String> dimensionIds = entry.getValue().stream().map(Dimension::getId).collect(Collectors.toList());
                query = query
                    .orNest()
                    .is(MenuGrantEntity::getDimensionTypeId, entry.getKey().toString())
                    .in(MenuGrantEntity::getDimensionId,dimensionIds)
                    .end();
            }
        }

        //执行查询并返回
        return query
            .fetch()
            //按菜单分组
            .collectMultimap(MenuGrantEntity::getMenuId, Function.identity());
    }
}
