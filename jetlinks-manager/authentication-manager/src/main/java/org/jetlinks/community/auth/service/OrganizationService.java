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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.springframework.context.event.EventListener;
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
     * @see DimensionUserBindUtils#bindUser(DefaultDimensionUserService, Collection, String, Collection, boolean)
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

    @EventListener
    public void doHandleSaved(EntitySavedEvent<OrganizationEntity> event) {
        event.async(
                Flux.fromIterable(event.getEntity())
                    .flatMap(this::handleParentId)
        );
    }

    @EventListener
    public void doHandleModified(EntityModifyEvent<OrganizationEntity> event) {
        event.async(
                Flux.fromIterable(event.getAfter())
                    .flatMap(this::handleParentId)
        );
    }

    private Mono<Void> handleParentId(OrganizationEntity organization) {
        return Mono
                .just(organization)
                .filter(org -> StringUtils.isBlank(organization.getParentId()))
                .flatMap(org -> createUpdate()
                        .setNull(OrganizationEntity::getParentId)
                        .where(OrganizationEntity::getId, organization.getId())
                        .execute()
                        .as(EntityEventHelper::setDoNotFireEvent)
                        .then()
                );
    }

}
