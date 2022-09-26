package org.jetlinks.community.auth.service;

import lombok.Generated;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单基础信息管理
 * <p>
 * 使用通用增删改查接口实现，同时实现通用树排序接口保证菜单数据的树状结构.
 *
 * @author wangzheng
 * @see MenuEntity
 * @since 1.0
 */
@Service
public class DefaultMenuService
    extends GenericReactiveCrudService<MenuEntity, String>
    implements ReactiveTreeSortEntityService<MenuEntity, String> {

    private final ReactiveRepository<MenuBindEntity, String> bindRepository;

    private final ApplicationEventPublisher eventPublisher;

    public DefaultMenuService(ReactiveRepository<MenuBindEntity, String> bindRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.bindRepository = bindRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Generated
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    @Generated
    public void setChildren(MenuEntity menuEntity, List<MenuEntity> children) {
        menuEntity.setChildren(children);
    }

    @Override
    @Generated
    public List<MenuEntity> getChildren(MenuEntity menuEntity) {
        return menuEntity.getChildren();
    }

    public Flux<MenuView> getMenuViews(QueryParamEntity queryParam, Predicate<MenuEntity> menuPredicate) {
        return this
            .createQuery()
            .setParam(queryParam.noPaging())
            .orderBy(SortOrder.asc(MenuEntity::getSortIndex))
            .fetch()
            .collectMap(MenuEntity::getId, Function.identity())
            .flatMapIterable(menus -> convertMenuView(menus, menuPredicate, MenuView::of));
    }

    /**
     * 根据维度获取已经授权的菜单信息
     *
     * @param dimensions 维度信息
     * @return 菜单信息
     */
    public Flux<MenuView> getGrantedMenus(QueryParamEntity queryParam, List<Dimension> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Flux.empty();
        }
        List<String> keyList = dimensions
            .stream()
            .map(dimension -> MenuBindEntity.generateTargetKey(dimension.getType().getId(), dimension.getId()))
            .collect(Collectors.toList());

        return bindRepository
            .createQuery()
            .setParam(queryParam.noPaging())
            .where()
            .in(MenuBindEntity::getTargetKey, keyList)
            .fetch()
            .as(this::convertToView);
    }

    public Flux<MenuView> getGrantedMenus(String dimensionType, String dimensionId) {
        return getGrantedMenus(dimensionType, Collections.singleton(dimensionId));
    }

    public Flux<MenuView> getGrantedMenus(String dimensionType, Collection<String> dimensionIds) {
        return bindRepository
            .createQuery()
            .where()
            .in(MenuBindEntity::getTargetKey, dimensionIds
                .stream()
                .map(dimensionId -> MenuBindEntity.generateTargetKey(dimensionType, dimensionId))
                .collect(Collectors.toSet()))
            .fetch()
            .as(this::convertToView);
    }


    private Flux<MenuView> convertToView(Flux<MenuBindEntity> entityFlux) {
        return Mono
            .zip(
                //全部菜单数据
                this
                    .createQuery()
                    .where()
                    .and(MenuEntity::getStatus, 1)
                    .fetch()
                    .collectMap(MenuEntity::getId, Function.identity(), LinkedHashMap::new),
                //菜单绑定信息
                entityFlux.collect(Collectors.groupingBy(MenuBindEntity::getMenuId)),
                (menus, binds) -> convertMenuView(menus,
                                                  menu -> binds.get(menu.getId()) != null,
                                                  menu -> MenuView.of(menu, binds.get(menu.getId())))
            )
            .flatMapIterable(Function.identity());
    }

    public Collection<MenuView> convertMenuView(Map<String, MenuEntity> menuMap,
                                                Predicate<MenuEntity> menuPredicate,
                                                Function<MenuEntity, MenuView> converter) {
        Map<String, MenuEntity> group = new HashMap<>();
        for (MenuEntity menu : menuMap.values()) {
            if (group.containsKey(menu.getId())) {
                continue;
            }
            if (menuPredicate.test(menu)) {
                String parentId = menu.getParentId();
                MenuEntity parent;
                group.put(menu.getId(), menu);
                //有子菜单默认就有父菜单
                while (StringUtils.hasText(parentId)) {
                    parent = menuMap.get(parentId);
                    if (parent == null) {
                        break;
                    }
                    parentId = parent.getParentId();
                    group.put(parent.getId(), parent);
                }
            }
        }
        return group
            .values()
            .stream()
            .map(converter)
            .sorted()
            .collect(Collectors.toList());
    }

    @EventListener
    public void handleMenuEntity(EntityModifyEvent<MenuEntity> e) {
        e.async(
            ClearUserAuthorizationCacheEvent.all().publish(eventPublisher)
        );
    }

    @EventListener
    public void handleMenuEntity(EntityDeletedEvent<MenuEntity> e) {
        e.async(
            ClearUserAuthorizationCacheEvent.all().publish(eventPublisher)
        );
    }

    @EventListener
    public void handleMenuEntity(EntitySavedEvent<MenuEntity> e) {
        e.async(
            ClearUserAuthorizationCacheEvent.all().publish(eventPublisher)
        );
    }

}
