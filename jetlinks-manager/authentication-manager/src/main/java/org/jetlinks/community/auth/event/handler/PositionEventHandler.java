package org.jetlinks.community.auth.event.handler;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityBeforeDeleteEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.entity.PositionEntity;
import org.jetlinks.community.auth.entity.PositionRoleEntity;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.service.PositionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
@AllArgsConstructor
public class PositionEventHandler {

    private final PositionService positionService;
    @SuppressWarnings("all")
    private final ReactiveRepository<PositionRoleEntity, String> roleBindRepository;

    @EventListener
    public void handleDeleteEvent(EntityBeforeDeleteEvent<PositionEntity> event) {
        event.async(
            handleBeforeDelete(event.getEntity())
        );
    }

    private Mono<Void> handleBeforeDelete(List<PositionEntity> posList) {
        return Flux
            .fromIterable(posList)
            .map(PositionEntity::getId)
            .collectList()
            .flatMap(ids -> positionService
                .createQuery()
                .in(PositionEntity::getParentId, ids)
                .fetchOne()
                .flatMap(ignore -> Mono
                    .error(() -> new BusinessException.NoStackTrace("error.exists_subordinate_position")))
            );
    }

    @EventListener
    public void handleRoleEvent(EntityDeletedEvent<RoleEntity> event) {
        event.async(handleRoleEvent(event.getEntity()));
    }

    @EventListener
    public void handleRoleEvent(EntityModifyEvent<RoleEntity> event) {
        event.async(handleRoleEvent(event.getAfter()));
    }

    @EventListener
    public void handleRoleEvent(EntitySavedEvent<RoleEntity> event) {
        event.async(handleRoleEvent(event.getEntity()));
    }

    private Mono<Void> handleRoleEvent(Collection<RoleEntity> entities) {
        return roleBindRepository
            .createDelete()
            .where()
            .is(PositionRoleEntity::getRoleId,
                Collections2.transform(entities, RoleEntity::getId))
            .execute()
            .then();
    }

    /**
     * 删除组织时同步删除职位信息
     *
     * @param event 组织删除事件
     */
    @EventListener
    public void handleOrgEvent(EntityDeletedEvent<OrganizationEntity> event) {
        event.async(
            positionService
                .createDelete()
                .where()
                .in(PositionEntity::getOrgId, Lists.transform(event.getEntity(), OrganizationEntity::getId))
                .execute()
        );
    }


    /**
     * 删除职位时,同步删除职位绑定的角色信息
     *
     * @param event 组织删除事件
     */
    @EventListener
    public void handleEvent(EntityDeletedEvent<PositionEntity> event) {
        event.async(
            roleBindRepository
                .createDelete()
                .where()
                .in(PositionRoleEntity::getPositionId, Lists.transform(event.getEntity(), PositionEntity::getId))
                .execute()
        );
    }

}
