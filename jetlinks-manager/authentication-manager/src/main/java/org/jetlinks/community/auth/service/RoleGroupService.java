package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.id.IDGenerator;

import org.jetlinks.community.auth.entity.RoleGroupEntity;
import org.jetlinks.community.auth.entity.RoleEntity;
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


    /**
     * 分组下存在角色时不可删除
     */
    @EventListener
    public void handleEvent(EntityDeletedEvent<RoleGroupEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .map(RoleGroupEntity::getId)
                //默认分组不可删除
                .filter(id -> !DEFAULT_GROUP_ID.equals(id))
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
        Flux<RoleGroupDetailTree> groupDetails = this
                .query(groupParam)
                .map(RoleGroupDetailTree::of);
        return QueryHelper
                .combineOneToMany(
                        groupDetails,
                        RoleGroupDetailTree::getGroupId,
                        roleService.createQuery().setParam(roleParam),
                        RoleEntity::getGroupId,
                        RoleGroupDetailTree::setRoles
                );
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
        //兼容旧数据,空分组即为默认分组
        roleService
            .createUpdate()
            .set(RoleEntity::getGroupId, DEFAULT_GROUP_ID)
            .isNull(RoleEntity::getGroupId)
            .execute()
            .subscribe(ignore -> {
                       },
                       err -> log.error("init role groupId error", err));

    }

}
