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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.entity.RoleGroupEntity;
import org.jetlinks.community.auth.web.response.RoleGroupDetailTree;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class RoleGroupService extends GenericReactiveTreeSupportCrudService<RoleGroupEntity, String> implements CommandLineRunner {

    public static final String DEFAULT_GROUP_ID = "default_group";

    private final RoleService roleService;

    public static final String ROLE_GROUP_ID = "groupId";


    /**
     * 分组下存在角色时不可删除
     */
    @EventListener
    public void handleEvent(EntityDeletedEvent<RoleGroupEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .map(RoleGroupEntity::getId)
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .flatMapMany(ids -> roleService
                    .createQuery()
                    .in(RoleEntity::getGroupId, ids)
                    .count()
                    .filter(i -> i <= 0)
                    .switchIfEmpty(Mono.error(() -> new I18nSupportException("error.group_role_exists"))))
        );
    }


    public Flux<RoleGroupDetailTree> queryDetailTree(QueryParamEntity groupParam, QueryParamEntity roleParam) {
        groupParam.setPaging(false);
        roleParam.setPaging(false);
        return LocaleUtils
            .currentReactive()
            .flatMapMany(locale -> this
                .query(groupParam)
                .map(tree -> RoleGroupDetailTree.of(tree, locale)))
            .as(fluxTree -> QueryHelper
                .combineOneToMany(
                    fluxTree,
                    RoleGroupDetailTree::getGroupId,
                    roleService.createQuery().setParam(roleParam),
                    RoleEntity::getGroupId,
                    RoleGroupDetailTree::setRoles
                ));
    }


    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.SNOW_FLAKE_STRING;
    }

    @Override
    public void setChildren(RoleGroupEntity entity, List<RoleGroupEntity> children) {
        entity.setChildren(children);
    }


    /**
     * 兼容旧数据
     */
    @Override
    public void run(String... args) {
        RoleGroupEntity groupEntity = new RoleGroupEntity();
        groupEntity.setName("默认");
        groupEntity.setId(DEFAULT_GROUP_ID);
        //变更为由auto-init下的json文件初始化数据
//        this
//            .save(groupEntity)
//            .then(roleService
//                      .createUpdate()
//                      .set(RoleEntity::getGroupId, DEFAULT_GROUP_ID)
//                      .isNull(RoleEntity::getGroupId)
//                      .execute())
//            .subscribe(ignore -> {
//                       },
//                       err -> log.error("init role groupId error", err));

    }

}
