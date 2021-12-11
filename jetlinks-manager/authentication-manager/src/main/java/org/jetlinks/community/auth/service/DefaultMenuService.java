package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.ReactiveTreeSortEntityService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.MenuButtonInfo;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuGrantEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.enums.MenuScope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单基础信息管理
 * <p>
 * 使用通用增删改查接口实现，同时实现通用树排序接口保证菜单数据的树状结构.
 *
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class DefaultMenuService
    extends GenericReactiveCrudService<MenuEntity, String>
    implements ReactiveTreeSortEntityService<MenuEntity, String> {
    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    @Override
    public void setChildren(MenuEntity menuEntity, List<MenuEntity> children) {
        menuEntity.setChildren(children);
    }

    @Override
    public List<MenuEntity> getChildren(MenuEntity menuEntity) {
        return menuEntity.getChildren();
    }

    /**
     * @param menuScope
     * @return
     */
    public ReactiveQuery<MenuEntity> getValidQuery(MenuScope menuScope) {

        ReactiveQuery<MenuEntity> query = this.createQuery()
            .where(MenuEntity::getStatus, 1);

        if (menuScope != null) {
            query = query.and(MenuEntity::getScope, menuScope);
        }

        return query;
    }


    public Flux<MenuView> listToTree(Flux<MenuView> flux) {
        return flux
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity
                .list2tree(list,
                    MenuView::setChildren,
                    (Predicate<MenuView>) n ->
                        StringUtils.isEmpty(n.getParentId())
                            || "-1".equals(n.getParentId())));
    }

    public Collection<MenuView> convertMenuView(Map<String, MenuEntity> menuMap,
                                                Map<String, Collection<MenuGrantEntity>> menuGrantEntityMap,
                                                Predicate<MenuEntity> menuPredicate,
                                                Predicate<MenuButtonInfo> buttonPredicate) {
        Map<String, MenuEntity> group = new HashMap<>();
        for (MenuEntity menu : menuMap.values()) {
            if (group.containsKey(menu.getId())) {
                continue;
            }
            if (menuGrantEntityMap != null) {
                if (!menuGrantEntityMap.containsKey(menu.getId())) {
                    continue;
                } else {
                    final Collection<MenuGrantEntity> menuGrantEntities = menuGrantEntityMap.get(menu.getId());
                    if (!CollectionUtils.isEmpty(menu.getButtons())) {
                        //过滤掉没有授权的按钮
                        final List<MenuButtonInfo> infos = menu.getButtons().stream().filter(it ->
                            menuGrantEntities.stream()
                                .anyMatch(menuGrantEntity -> it.getId().equals(menuGrantEntity.getButtonId())))
                            .collect(Collectors.toList());
                        menu.setButtons(infos);
                    }
                }
            }
            if (menuPredicate.test(menu)) {
                String parentId = menu.getParentId();
                MenuEntity parent;
                group.put(menu.getId(), menu);
                //有子菜单默认就有父菜单
                while (!StringUtils.isEmpty(parentId)) {
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
            .map(menu -> MenuView.of(menu.copy(buttonPredicate)))
            .sorted()
            .collect(Collectors.toList());
    }

    public Collection<MenuView> convertMenuView(Map<String, MenuEntity> menuMap) {
        Map<String, MenuEntity> group = new HashMap<>();
        for (MenuEntity menu : menuMap.values()) {
            if (group.containsKey(menu.getId())) {
                continue;
            }

            String parentId = menu.getParentId();
            MenuEntity parent;
            group.put(menu.getId(), menu);
            //有子菜单默认就有父菜单
            while (!StringUtils.isEmpty(parentId)) {
                parent = menuMap.get(parentId);
                if (parent == null) {
                    break;
                }
                parentId = parent.getParentId();
                group.put(parent.getId(), parent);
            }
        }
        return group
            .values()
            .stream()
            .map(menu -> MenuView.of(menu))
            .sorted()
            .collect(Collectors.toList());
    }
}
