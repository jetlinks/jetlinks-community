package org.jetlinks.community.auth.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Function;

public class DimensionUserBindUtils {

    /**
     * 绑定用户到指定的维度中,removeOldBind设置为true时,在绑定前会删除旧的绑定信息（全量绑定）.
     * 否则不会删除旧的绑定信息(增量绑定)
     *
     * @param userIdList      用户ID列表
     * @param dimensionType   维度类型,
     *                        如:角色{@link  DefaultDimensionType#role},部门{@link  OrgDimensionType#org}.
     * @param dimensionIdList 角色ID列表
     * @param removeOldBind   是否删除旧的绑定信息
     * @return void
     */
    public static Mono<Void> bindUser(DefaultDimensionUserService dimensionUserService,
                                      Collection<String> userIdList,
                                      String dimensionType,
                                      Collection<String> dimensionIdList,
                                      boolean removeOldBind,
                                      Function<ReactiveDelete, ReactiveDelete> deleteCustomizer,
                                      Function<DimensionUserEntity, DimensionUserEntity> customizer) {

        Mono<Void> before = Mono.empty();
        if (removeOldBind) {
            before = deleteCustomizer
                .apply(
                    dimensionUserService
                        .createDelete()
                        .where()
                        .in(DimensionUserEntity::getUserId, userIdList)
                        .and(DimensionUserEntity::getDimensionTypeId, dimensionType)
                )
                .execute()
                .then();
        }
        if (CollectionUtils.isEmpty(dimensionIdList)) {
            return before;
        }

        return before.then(
            Flux
                .fromIterable(userIdList)
                .flatMap(userId -> Flux
                    .fromIterable(dimensionIdList)
                    .mapNotNull(dimensionId -> customizer.apply(createEntity(dimensionType, dimensionId, userId))))
                .as(dimensionUserService::save)
                .then()
        );
    }

    /**
     * 绑定用户到指定的维度中,removeOldBind设置为true时,在绑定前会删除旧的绑定信息（全量绑定）.
     * 否则不会删除旧的绑定信息(增量绑定)
     *
     * @param userIdList      用户ID列表
     * @param dimensionType   维度类型,
     *                        如:角色{@link  DefaultDimensionType#role},部门{@link  OrgDimensionType#org}.
     * @param dimensionIdList 角色ID列表
     * @param removeOldBind   是否删除旧的绑定信息
     * @return void
     */
    public static Mono<Void> bindUser(DefaultDimensionUserService dimensionUserService,
                                      Collection<String> userIdList,
                                      String dimensionType,
                                      Collection<String> dimensionIdList,
                                      boolean removeOldBind) {
        return bindUser(dimensionUserService, userIdList, dimensionType, dimensionIdList, removeOldBind,Function.identity(), Function.identity());
    }

    public static Mono<Integer> unbindUser(DefaultDimensionUserService dimensionUserService,
                                           Collection<String> userIdList,
                                           String dimensionType,
                                           Collection<String> dimensionIdList,
                                           Function<ReactiveDelete,ReactiveDelete> customizer) {
        return dimensionUserService
            .createDelete()
            .where(DimensionUserEntity::getDimensionTypeId, dimensionType)
            .when(CollectionUtils.isNotEmpty(userIdList),
                  delete -> delete.in(DimensionUserEntity::getUserId, userIdList))
            .when(CollectionUtils.isNotEmpty(dimensionIdList),
                  delete -> delete.in(DimensionUserEntity::getDimensionId, dimensionIdList))
            .execute();
    }
    public static Mono<Integer> unbindUser(DefaultDimensionUserService dimensionUserService,
                                           Collection<String> userIdList,
                                           String dimensionType,
                                           Collection<String> dimensionIdList) {
        return unbindUser(dimensionUserService, userIdList, dimensionType, dimensionIdList,Function.identity());
    }


    private static DimensionUserEntity createEntity(String dimensionType, String dimensionId, String userId) {
        DimensionUserEntity entity = new DimensionUserEntity();
        entity.setUserId(userId);
        entity.setUserName(userId);
        entity.setDimensionName(dimensionId);
        entity.setDimensionTypeId(dimensionType);
        entity.setDimensionId(dimensionId);
        entity.generateId();
        return entity;
    }

}
