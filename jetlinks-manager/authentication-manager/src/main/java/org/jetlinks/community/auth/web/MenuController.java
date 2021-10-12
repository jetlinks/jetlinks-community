package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.MenuButtonInfo;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.service.AuthorizationSettingDetailService;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单管理
 *
 * @author wangzheng
 * @since 1.0
 */
@RestController
@RequestMapping("/menu")
@Authorize
@Resource(id = "menu", name = "菜单管理", group = "system")
@Tag(name = "菜单管理")
@AllArgsConstructor
public class MenuController implements ReactiveServiceCrudController<MenuEntity, String> {

    private final DefaultMenuService defaultMenuService;

    private final AuthorizationSettingDetailService settingService;

    @Override
    public ReactiveCrudService<MenuEntity, String> getService() {
        return defaultMenuService;
    }

    /**
     * 获取用户自己的菜单列表
     *
     * @return 菜单列表
     */
    @GetMapping("/user-own/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(树结构)")
    public Flux<MenuView> getUserMenuAsTree() {
        return this
            .getUserMenuAsList()
            .as(MenuController::listToTree);
    }


    @GetMapping("/user-own/list")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(列表结构)")
    public Flux<MenuView> getUserMenuAsList() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(autz -> defaultMenuService
                .createQuery()
                .where(MenuEntity::getStatus,1)
                .fetch()
                .collect(Collectors.toMap(MenuEntity::getId, Function.identity()))
                .flatMapIterable(menuMap -> MenuController
                    .convertMenuView(menuMap,
                                     menu -> "admin".equals(autz.getUser().getUsername()) ||
                                         menu.hasPermission(autz::hasPermission),
                                     button -> "admin".equals(autz.getUser().getUsername()) ||
                                         button.hasPermission(autz::hasPermission)
                    )));
    }

    @PutMapping("/_grant")
    @Operation(summary = "根据菜单进行授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> grant(@RequestBody Mono<MenuGrantRequest> body) {
        return Mono
            .zip(
                //T1: 当前用户权限信息
                Authentication.currentReactive(),
                //T2: 将菜单信息转为授权信息
                Mono
                    .zip(body,
                         defaultMenuService
                             .createQuery()
                             .where(MenuEntity::getStatus,1)
                             .fetch()
                             .collectList(),
                         MenuGrantRequest::toAuthorizationSettingDetail
                    )
                    .map(Flux::just),
                //保存授权信息
                settingService::saveDetail
            )
            .flatMap(Function.identity());
    }

    @GetMapping("/{targetType}/{targetId}/_grant/tree")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取菜单授权信息(树结构)")
    public Flux<MenuView> getGrantInfoTree(@PathVariable String targetType,
                                           @PathVariable String targetId) {

        return this
            .getGrantInfo(targetType, targetId)
            .as(MenuController::listToTree);
    }

    @GetMapping("/{targetType}/{targetId}/_grant/list")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取菜单授权信息(列表结构)")
    public Flux<MenuView> getGrantInfo(@PathVariable String targetType,
                                       @PathVariable String targetId) {

        return Mono
            .zip(
                //权限设置信息
                settingService.getSettingDetail(targetType, targetId),
                //菜单
                defaultMenuService
                    .createQuery()
                    .where(MenuEntity::getStatus,1)
                    .fetch()
                    .collectMap(MenuEntity::getId, Function.identity()),
                (detail, menuMap) -> MenuController
                    .convertMenuView(menuMap,
                                     menu -> menu.hasPermission(detail::hasPermission),
                                     button -> button.hasPermission(detail::hasPermission)
                    )
            )
            .flatMapIterable(Function.identity());
    }

    private static Flux<MenuView> listToTree(Flux<MenuView> flux) {
        return flux
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity
                .list2tree(list,
                           MenuView::setChildren,
                           (Predicate<MenuView>) n ->
                               StringUtils.isEmpty(n.getParentId())
                                   || "-1".equals(n.getParentId())));
    }

    private static Collection<MenuView> convertMenuView(Map<String, MenuEntity> menuMap,
                                                        Predicate<MenuEntity> menuPredicate,
                                                        Predicate<MenuButtonInfo> buttonPredicate) {
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

}
