package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.AuthenticationUtils;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单管理
 * @author wangzheng
 * @since 1.0
 */
@RestController
@RequestMapping("/menu")
@Authorize
@Resource(id = "menu", name = "菜单管理", group = "system")
@Hidden
public class MenuController implements ReactiveServiceCrudController<MenuEntity, String> {

    @Autowired
    private DefaultMenuService defaultMenuService;

    @Override
    public ReactiveCrudService<MenuEntity, String> getService() {
        return defaultMenuService;
    }

    public Collection<MenuEntity> predicateUserMenu(Map<String, MenuEntity> menuMap, Authentication autz) {
        Map<String, MenuEntity> group = new HashMap<>();
        for (MenuEntity menu : menuMap.values()) {
            if (group.containsKey(menu.getId())) {
                continue;
            }
            if (autz.getUser().getUsername().equals("admin") || AuthenticationUtils.createPredicate(menu.getPermissionExpression()).test(autz)) {
                String parentId = menu.getParentId();
                MenuEntity parent;
                group.put(menu.getId(), menu);
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
        List<MenuEntity> list = new ArrayList<>(group.values());
        Collections.sort(list);

        return list;
    }

    /**
     * 获取用户自己的菜单列表
     * @return 菜单列表
     */
    @GetMapping("user-own/tree")
    @Authorize(merge = false)
    public Flux<MenuEntity> getUserMenuAsTree() {
        return Authentication
                .currentReactive()
                .switchIfEmpty(Mono.error(UnAuthorizedException::new))
                .flatMapMany(autz -> defaultMenuService
                        .createQuery()
                        .fetch()
                        .collect(Collectors.toMap(MenuEntity::getId, Function.identity()))
                        .map(menuMap -> predicateUserMenu(menuMap, autz))
                        .map(menus -> TreeSupportEntity.list2tree(
                                menus,
                                MenuEntity::setChildren,
                                (Predicate<MenuEntity>) n ->
                                        StringUtils.isEmpty(n.getParentId())
                                                || "-1".equals(n.getParentId()))).flatMapMany(Flux::fromIterable));

    }
}
