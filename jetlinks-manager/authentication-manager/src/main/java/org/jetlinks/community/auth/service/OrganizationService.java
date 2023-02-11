package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.dimension.OrgDimensionType;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class OrganizationService extends GenericReactiveTreeSupportCrudService<OrganizationEntity, String> {

    private DefaultDimensionUserService dimensionUserService;

    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.UUID;
    }

    @Override
    public void setChildren(OrganizationEntity entity, List<OrganizationEntity> children) {
        entity.setChildren(children);
    }

    @Transactional
    public Mono<Integer> bindUser(String orgId, List<String> userIdList) {
        Flux<String> userIdStream = Flux.fromIterable(userIdList);

        return this
            .findById(orgId)
            .flatMap(org -> userIdStream
                .map(userId -> {
                    DimensionUserEntity userEntity = new DimensionUserEntity();
                    userEntity.setUserId(userId);
                    userEntity.setUserName(userId);
                    userEntity.setDimensionId(orgId);
                    userEntity.setDimensionTypeId(OrgDimensionType.org.getId());
                    userEntity.setDimensionName(org.getName());
                    return userEntity;
                })
                .as(dimensionUserService::save))
            .map(SaveResult::getTotal);

    }

    @Transactional
    public Mono<Integer> unbindUser(String orgId, List<String> userIdList) {
        Flux<String> userIdStream = Flux.fromIterable(userIdList);

        return userIdStream
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(newUserIdList -> dimensionUserService
                .createDelete()
                .where(DimensionUserEntity::getDimensionTypeId, OrgDimensionType.org.getId())
                .in(DimensionUserEntity::getUserId, newUserIdList)
                .and(DimensionUserEntity::getDimensionId, orgId)
                .execute())
            ;
    }


    /**
     * 绑定用户到机构(部门)
     *
     * @param userIdList    用户ID
     * @param orgIdList     机构Id
     * @param removeOldBind 是否删除旧的绑定信息
     * @return void
     */
    @Transactional
    public Mono<Void> bindUser(Collection<String> userIdList,
                               Collection<String> orgIdList,
                               boolean removeOldBind) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }
        return DimensionUserBindUtils.bindUser(dimensionUserService, userIdList, OrgDimensionType.org.getId(), orgIdList, removeOldBind);
    }



}
