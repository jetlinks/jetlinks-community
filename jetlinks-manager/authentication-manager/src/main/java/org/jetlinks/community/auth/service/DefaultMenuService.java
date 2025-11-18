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

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.cache.ReactiveCacheManager;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.jetlinks.community.auth.configuration.MenuProperties;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import java.time.Duration;
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
@Slf4j
public class DefaultMenuService
    extends GenericReactiveCrudService<MenuEntity, String>
    implements ReactiveTreeSortEntityService<MenuEntity, String> {

    private final ReactiveRepository<MenuBindEntity, String> bindRepository;


    private final ApplicationEventPublisher eventPublisher;

    private final ReactiveCacheManager cacheManager;

    private final MenuProperties properties;


    public static final String MENU_CACHE_KEY = "menus-cache";

    public DefaultMenuService(ReactiveRepository<MenuBindEntity, String> bindRepository,
                              ApplicationEventPublisher eventPublisher,
                              ReactiveCacheManager cacheManager,
                              MenuProperties properties1) {
        this.bindRepository = bindRepository;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
        this.properties = properties1;
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

    public Flux<MenuView> getMenuViews(String targetType, String targetId, Flux<MenuView> menus) {
        Flux<MenuView> allMenu = menus.cache();
        return Mono
            .zip(
                this
                    .getGrantedMenus(targetType, targetId)
                    .collectMap(GenericEntity::getId),
                allMenu.collectMap(MenuView::getId, Function.identity(), LinkedHashMap::new),
                (granted, all) -> LocaleUtils
                    .currentReactive()
                    .flatMapMany(locale -> allMenu
                        .doOnNext(MenuView::resetGrant)
                        .map(view -> view
                            .withGranted(granted.get(view.getId()))
                        )))
            .flatMapMany(Function.identity());
    }

    public Flux<MenuView> getMenuViews(QueryParamEntity queryParam, Predicate<MenuEntity> menuPredicate) {
        return this
            .createQuery()
            .setParam(queryParam.noPaging())
            .fetch()
            .collectMap(MenuEntity::getId, Function.identity(), LinkedHashMap::new)
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

    final Map<List<String>, Flux<MenuView>> grantedCaching =
        Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofSeconds(10))
            .<List<String>, Flux<MenuView>>build()
            .asMap();

    public Flux<MenuView> getGrantedMenus(List<Dimension> dimensions,
                                          Mono<Map<String, MenuEntity>> menuEntityMap) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Flux.empty();
        }
        List<String> keyList = dimensions
            .stream()
            .filter(this::isMenuDimension)
            .map(dimension -> MenuBindEntity.generateTargetKey(dimension.getType().getId(), dimension.getId()))
            .sorted()
            .collect(Collectors.toList());

        return grantedCaching
            .computeIfAbsent(keyList,
                             _keyList -> Flux
                                 .defer(() -> this
                                     .convertToView(CollectionUtils.isEmpty(_keyList)
                                                        ? Flux.empty()
                                                        : bindRepository
                                                        .createQuery()
                                                        .where()
                                                        .in(MenuBindEntity::getTargetKey, _keyList)
                                                        .fetch(),
                                                    menuEntityMap))
                                 // 缓存1秒钟,避免编辑角色或者组织时,导致大量用户权限失效并进行初始化时执行大量重复的查询.
                                 .cache(Duration.ofSeconds(1)));


    }

    private boolean isMenuDimension(Dimension dimension) {
        //是配置中的维度并且没有忽略
        return properties.getDimensions().contains(dimension.getType().getId());
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


    private Flux<MenuView> convertToView(Flux<MenuBindEntity> entityFlux, Mono<Map<String, MenuEntity>> menuEntityMap) {
        return Mono
            .zip(
                //全部菜单数据
                menuEntityMap,
                //菜单绑定信息
                entityFlux.collect(Collectors.groupingBy(MenuBindEntity::getMenuId)),
                (menus, binds) -> convertMenuView(menus,
                                                  menu -> binds.get(menu.getId()) != null,
                                                  menu -> MenuView.of(menu, binds.get(menu.getId())))
            )
            .flatMapIterable(Function.identity());
    }

    public Mono<Map<String, MenuEntity>> getEnabledMenus() {
        return getCacheMenus()
            .filter(menu -> ObjectUtils.equals((byte) 1, menu.getStatus()))
            .collectMap(MenuEntity::getId, Function.identity(), LinkedHashMap::new);
    }

    protected Flux<MenuEntity> getCacheMenus() {
        return cacheManager
            .<MenuEntity>getCache(MENU_CACHE_KEY)
            .getFlux(MENU_CACHE_KEY, () -> this
                .createQuery()
                .fetch()
            );
    }

    public Mono<Map<String, MenuEntity>> getEnabledMenus(QueryParamEntity queryParam) {
        return this
            .createQuery()
            .setParam(queryParam.noPaging())
            .where()
            .and(MenuEntity::getStatus, 1)
            .fetch()
            .collectMap(MenuEntity::getId, Function.identity(), LinkedHashMap::new);
    }

    private Flux<MenuView> convertToView(Flux<MenuBindEntity> entityFlux) {
        return convertToView(entityFlux, getEnabledMenus());
    }

    public Collection<MenuView> convertMenuView(Map<String, MenuEntity> menuMap,
                                                Predicate<MenuEntity> menuPredicate,
                                                Function<MenuEntity, MenuView> converter) {

        List<MenuEntity> menus = new ArrayList<>(menuMap.values());
        Map<String, Integer> menuSort = new HashMap<>();
        Map<String, MenuEntity> group = new HashMap<>();

        for (int i = 0; i < menus.size(); i++) {
            MenuEntity menu = menus.get(i);
            menuSort.put(menu.getId(), i);
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
            evictCacheMenus()
                .then(ClearUserAuthorizationCacheEvent.all().publish(eventPublisher))
        );
    }

    @EventListener
    public void handleMenuEntity(EntityDeletedEvent<MenuEntity> e) {
        e.async(
            evictCacheMenus()
                .then(ClearUserAuthorizationCacheEvent.all().publish(eventPublisher))
        );
    }

    @EventListener
    public void handleMenuEntity(EntitySavedEvent<MenuEntity> e) {
        e.async(
            evictCacheMenus()
                .then(ClearUserAuthorizationCacheEvent.all().publish(eventPublisher))

        );
    }

    private Mono<Void> evictCacheMenus() {
        return cacheManager
            .getCache(MENU_CACHE_KEY)
            .evict(MENU_CACHE_KEY);
    }

    public Flux<MenuView> getUserMenuAsList(Authentication auth, QueryParamEntity queryParam) {
        if (queryParam.getSorts().isEmpty()) {
            queryParam.toQuery().orderByAsc(MenuEntity::getSortIndex);
        }
        return properties.isAllowAllMenu(auth)
            ? this
            .getMenuViews(queryParam, menu -> true)
            .doOnNext(MenuView::grantAll)
            : this
            .getGrantedMenus(queryParam, auth.getDimensions());
    }

}
