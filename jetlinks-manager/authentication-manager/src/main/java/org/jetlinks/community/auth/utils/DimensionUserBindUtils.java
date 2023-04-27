package org.jetlinks.community.auth.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.dimension.OrgDimensionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

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
                                      boolean removeOldBind) {

        Mono<Void> before = Mono.empty();
        if (removeOldBind) {
            before = dimensionUserService
                .createDelete()
                .where()
                .in(DimensionUserEntity::getUserId, userIdList)
                .and(DimensionUserEntity::getDimensionTypeId, dimensionType)
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
                    .map(dimensionId -> createEntity(dimensionType, dimensionId, userId)))
                .as(dimensionUserService::save)
                .then()
        );

    }

    public static Mono<Void> unbindUser(DefaultDimensionUserService dimensionUserService,
                                        Collection<String> userIdList,
                                        String dimensionType,
                                        Collection<String> dimensionIdList) {
        return dimensionUserService
            .createDelete()
            .where()
            .in(DimensionUserEntity::getUserId, userIdList)
            .and(DimensionUserEntity::getDimensionTypeId, dimensionType)
            .when(CollectionUtils.isNotEmpty(dimensionIdList),
                  delete -> delete.in(DimensionUserEntity::getDimensionId, dimensionIdList))
            .execute()
            .then();
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
