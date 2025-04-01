package org.jetlinks.community.auth.dimension;

import com.google.common.collect.Collections2;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.hswebframework.web.system.authorization.defaults.service.terms.DimensionTerm;
import org.jetlinks.community.auth.configuration.AuthenticationDimensionProperties;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.entity.PositionEntity;
import org.jetlinks.community.auth.entity.PositionRoleEntity;
import org.jetlinks.community.auth.service.OrganizationService;
import org.jetlinks.community.auth.service.PositionService;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PositionDimensionProvider extends BaseDimensionProvider<PositionEntity> {

    private final PositionService service;
    private final RoleDimensionProvider roleProvider;
    private final OrganizationService organizationService;
    private final AuthenticationDimensionProperties properties;

    public PositionDimensionProvider(PositionService service,
                                     RoleDimensionProvider roleProvider,
                                     DefaultDimensionUserService dimensionUserService,
                                     OrganizationService organizationService,
                                     ApplicationEventPublisher eventPublisher,
                                     AuthenticationDimensionProperties properties) {
        super(service.getRepository(), eventPublisher, dimensionUserService);
        this.service = service;
        this.organizationService = organizationService;
        this.roleProvider = roleProvider;
        this.properties = properties;
    }

    @Override
    protected DimensionType getDimensionType() {
        return OrgDimensionType.position;
    }

    @Override
    protected Mono<Dimension> convertToDimension(PositionEntity entity) {
        // todo 数据权限控制?
        return Mono.just(entity.toDimension(true));
    }

    @Override
    protected boolean isChanged(PositionEntity before, PositionEntity after) {
        //名称变化或者parentId变化才需要更新
        return !Objects.equals(before.getName(), after.getName())
            || !Objects.equals(before.getParentId(), after.getParentId());
    }

    @Override
    protected Class<?> getEntityType() {
        return PositionEntity.class;
    }

    @Override
    public Flux<? extends Dimension> getDimensionByUserId(String userId) {
        Map<String, Dimension> dimensions = new LinkedHashMap<>();
        Map<String, Dimension> orgDimensions = new LinkedHashMap<>();

        return DimensionTerm
            .inject(createQuery(), "id", getDimensionType().getId(), Collections.singletonList(userId))
            .fetch()
            .map(PositionEntity::getId)
            .collect(Collectors.toSet())
            //从缓存中获取,提升性能
            .flatMapIterable(service::getCachedTree)
            .doOnNext(entity -> {
                //直接关联的职位
                //todo 数据权限控制?
                dimensions.put(entity.getId(), entity.toDimension(true));
                //下级?
                //entity.getChildren()

                //上级职位.
                String pid = entity.getParentId();
                while (pid != null) {
                    PositionEntity parent = service.getCached(pid).orElse(null);
                    if (parent != null) {
                        dimensions.putIfAbsent(parent.getId(), parent.toParentDimension());
                        pid = parent.getParentId();
                    } else {
                        break;
                    }
                }

                if (properties.getPosition().isAutoBindOrg()) {
                    //自动关联组织
                    List<OrganizationEntity> orgList = organizationService
                        .getCachedTree(Collections.singleton(entity.getOrgId()));
                    for (OrganizationEntity org : orgList) {
                        OrganizationDimensionProvider.applyDimensions(
                            organizationService,
                            org,
                            orgDimensions,
                            Collections.singletonMap("positionId", entity.getId())
                        );
                    }
                }
            })
            .thenMany(Flux.defer(() -> Flux
                .concat(
                    //返回职位绑定的角色信息
                    properties.getPosition().isAutoBindRole()
                        ? service
                        .getPositionRoles(dimensions
                                              .entrySet()
                                              .stream()
                                              .filter(dim -> OrgDimensionType
                                                  .position
                                                  .isSameType(dim.getValue().getType()))
                                              .map(Map.Entry::getKey)
                                              .collect(Collectors.toSet()))
                        .flatMap(e -> roleProvider.convertToDimension(e.getRole())) :
                    Flux.empty(),
                    //返回职位自身
                    Flux.fromIterable(dimensions.values()),
                    //关联的组织信息
                    Flux.fromIterable(orgDimensions.values())
                )));
    }

    //* =========  角色绑定信息相关事件 =============
    @EventListener
    public void handleRoleBindEvent(EntitySavedEvent<PositionRoleEntity> event) {

        event.async(
            clearUserAuthenticationCacheById(
                Collections2.transform(event.getEntity(),
                                       PositionRoleEntity::getPositionId)
            )
        );
    }

    @EventListener
    public void handleRoleBindEvent(EntityDeletedEvent<PositionRoleEntity> event) {
        event.async(
            clearUserAuthenticationCacheById(Collections2.transform(event.getEntity(), PositionRoleEntity::getPositionId))
        );
    }

    @EventListener
    public void handleRoleBindEvent(EntityModifyEvent<PositionRoleEntity> event) {

        Set<String> id = new HashSet<>();
        id.addAll(Collections2.transform(event.getBefore(), PositionRoleEntity::getPositionId));
        id.addAll(Collections2.transform(event.getAfter(), PositionRoleEntity::getPositionId));
        event.async(
            clearUserAuthenticationCacheById(id)
        );
    }


    @EventListener
    public void handleRoleBindEvent(EntityCreatedEvent<PositionRoleEntity> event) {

        event.async(
            clearUserAuthenticationCacheById(
                Collections2.transform(event.getEntity(),
                                       PositionRoleEntity::getPositionId)
            )
        );
    }

    @Override
    protected Mono<Void> clearUserAuthenticationCache(Collection<PositionEntity> entities) {
        //清空下级权限缓存,因为权限中会记录上级信息.
        return service
            .queryIncludeChildren(Collections2.transform(entities, PositionEntity::getId))
            .collectList()
            .flatMap(super::clearUserAuthenticationCache)
            .as(ClearUserAuthorizationCacheEvent::doOnEnabled);
    }


    @EventListener
    public void handleCreatEvent(EntityCreatedEvent<PositionEntity> event) {
        event.async(
            clearUserAuthenticationCache(event.getEntity())
        );
    }

}
