package org.jetlinks.community.auth.dimension;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.cache.ReactiveCache;
import org.hswebframework.web.cache.ReactiveCacheManager;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.enums.RoleState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Objects;

@Component
public class RoleDimensionProvider extends BaseDimensionProvider<RoleEntity> {

    private final ReactiveCache<Dimension> cache;

    public RoleDimensionProvider(ReactiveRepository<RoleEntity, String> repository,
                                 DefaultDimensionUserService dimensionUserService,
                                 ApplicationEventPublisher eventPublisher,
                                 ReactiveCacheManager cacheManager) {
        super(repository, eventPublisher, dimensionUserService);
        this.cache = cacheManager.getCache("role-dimension");
    }

    @Override
    protected DimensionType getDimensionType() {
        return DefaultDimensionType.role;
    }

    @Override
    protected Mono<Dimension> convertToDimension(RoleEntity entity) {

        return Mono.just(entity.toDimension());
    }

    @Override
    protected Class<?> getEntityType() {
        return RoleEntity.class;
    }

    @EventListener
    public void handleMenuChanged(EntitySavedEvent<MenuEntity> event) {
        event.async(cleanMenuCache(event.getEntity()));
    }

    @EventListener
    public void handleMenuChanged(EntityModifyEvent<MenuEntity> event) {
        event.async(cleanMenuCache(event.getAfter()));
    }

    @EventListener
    public void handleMenuChanged(EntityDeletedEvent<MenuEntity> event) {
        event.async(cleanMenuCache(event.getEntity()));
    }

    @Override
    @EventListener
    public void handleEvent(EntitySavedEvent<RoleEntity> event) {
        super.handleEvent(event);
        event.async(cleanCache(event.getEntity()));
    }

    @Override
    @EventListener
    public void handleEvent(EntityModifyEvent<RoleEntity> event) {
        super.handleEvent(event);
        event.async(cleanCache(event.getAfter()));
    }

    @Override
    protected boolean isChanged(RoleEntity before, RoleEntity after) {
        //名称
        return !Objects.equals(before.getName(), after.getName())
            || !Objects.equals(before.getState(), after.getState());
    }

    @Override
    @EventListener
    public void handleEvent(EntityDeletedEvent<RoleEntity> event) {
        super.handleEvent(event);
        event.async(cleanCache(event.getEntity()));
    }

    private Mono<Void> cleanMenuCache(Collection<MenuEntity> menuBinds) {
        return cache.clear();
    }

    private Mono<Void> cleanCache(Collection<RoleEntity> roleId) {
        return Flux.fromIterable(roleId)
                   .map(RoleEntity::getId)
                   .filter(StringUtils::hasText)
                   .collectList()
                   .filter(CollectionUtils::isNotEmpty)
                   .flatMap(cache::evictAll);
    }

    @Override
    protected ReactiveQuery<RoleEntity> createQuery() {
        return super
            .createQuery()
            .and(RoleEntity::getState, RoleState.enabled.getValue());
    }

}
