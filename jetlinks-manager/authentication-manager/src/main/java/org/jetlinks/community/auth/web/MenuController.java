package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuGrantEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.enums.MenuScope;
import org.jetlinks.community.auth.service.AuthorizationSettingDetailService;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.service.MenuGrantService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    private final MenuGrantService menuGrantService;

    private final AuthorizationSettingDetailService settingService;

    @Override
    public ReactiveCrudService<MenuEntity, String> getService() {
        return defaultMenuService;
    }

    /**
     * 获取所有的菜单
     *
     * @return 菜单列表
     */
    @GetMapping("/all/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取所有的菜单(树结构)")
    public Flux<MenuView> getAllMenuAsTree(MenuScope menuScope) {
        return this
            .getAllMenuAsList(menuScope)
            .as(defaultMenuService::listToTree);
    }


    @GetMapping("/all/list")
    @Authorize(merge = false)
    @Operation(summary = "获取所有的菜单(列表结构)")
    public Flux<MenuView> getAllMenuAsList(MenuScope menuScope) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(autz -> defaultMenuService.getValidQuery(menuScope)
                .fetch()
                .collect(Collectors.toMap(MenuEntity::getId, Function.identity()))
                .flatMapIterable(menuMap -> defaultMenuService
                    .convertMenuView(menuMap,
                        null,
                        menu -> true,
                        button -> true
                    )));
    }

    /**
     * 获取用户自己的菜单列表
     *
     * @return 菜单列表
     */
    @GetMapping("/user-own/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(树结构)")
    public Flux<MenuView> getUserMenuAsTree(MenuScope menuScope) {
        return this
            .getUserMenuAsList(menuScope)
            .as(defaultMenuService::listToTree);
    }


    @GetMapping("/user-own/list")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(列表结构)")
    public Flux<MenuView> getUserMenuAsList(MenuScope menuScope) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(autz -> {
                final User user = autz.getUser();
                final boolean isAdmin = user.getUsername().equals("admin");
                if (isAdmin) {
                    return Mono.just(Tuples.of(autz, isAdmin, new HashMap()));
                }
                //获取菜单授权
                return menuGrantService
                    .getMenuGrantMap(user.getId(), autz.getDimensions(DefaultDimensionType.role))
                    .flatMap(it -> Mono.just(Tuples.of(autz, isAdmin, it)));
            })
            .flatMapMany(tuple3 -> {
                final Authentication autz = tuple3.getT1();
                final boolean isAdmin = tuple3.getT2();
                final Map<String, Collection<MenuGrantEntity>> menuGrantEntityMap = tuple3.getT3();
                return defaultMenuService.getValidQuery(menuScope)
                    .fetch()
                    .collect(Collectors.toMap(MenuEntity::getId, Function.identity()))
                    .flatMapIterable(menuMap ->
                    {
                        if (isAdmin) {
                            return defaultMenuService
                                .convertMenuView(menuMap);
                        } else {
                            return defaultMenuService
                                .convertMenuView(menuMap,
                                    menuGrantEntityMap,
                                    menu -> "admin".equals(autz.getUser().getUsername()) ||
                                        menu.hasPermission(autz::hasPermission),
                                    button -> "admin".equals(autz.getUser().getUsername()) ||
                                        button.hasPermission(autz::hasPermission)
                                );
                        }
                    });
            });
    }

    @PutMapping("/_grant")
    @Operation(summary = "根据菜单进行授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> grant(@RequestBody Mono<MenuGrantRequest> body) {

        Mono<MenuGrantRequest> menuGrantRequestMono = body.cache();

        //登录者授权信息
        final Mono<Authentication> authenticationMono = Authentication.currentReactive();
        //有效的菜单信息
        final Mono<List<MenuEntity>> menuEntitiesMono =
            menuGrantRequestMono.flatMap(it -> defaultMenuService
                .getValidQuery(it.getMenuScope())
                .fetch()
                .collectList()
                .cache());

        return
            menuGrantService.save(menuGrantRequestMono, menuEntitiesMono)
                .flatMap(it ->
                    //保存授权信息
                    settingService.saveDetail(
                        authenticationMono,
                        menuGrantRequestMono,
                        menuEntitiesMono)
                );
    }


    @GetMapping("/{targetType}/{targetId}/_grant/tree")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取菜单授权信息(树结构)", description = "无作用域限制")
    public Flux<MenuView> getGrantInfoTree(@PathVariable String targetType,
                                           @PathVariable String targetId) {

        return this
            .getGrantInfo(targetType, targetId)
            .as(defaultMenuService::listToTree);
    }

    @GetMapping("/{targetType}/{targetId}/{menuScope}/_grant/tree")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取指定作用域下菜单授权信息(树结构)", description = "有作用域限制")
    public Flux<MenuView> getScopeGrantInfoTree(@PathVariable String targetType,
                                                @PathVariable String targetId,
                                                @PathVariable String menuScope) {

        return this
            .getScopeGrantInfo(targetType, targetId, menuScope)
            .as(defaultMenuService::listToTree);
    }

    @GetMapping("/{targetType}/{targetId}/_grant/list")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取菜单授权信息(列表结构)", description = "无作用域限制")
    public Flux<MenuView> getGrantInfo(@PathVariable String targetType,
                                       @PathVariable String targetId) {
        return getGrantInfo(targetType, targetId, null);
    }

    @GetMapping("/{targetType}/{targetId}/{menuScope}/_grant/list")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "获取指定作用域下菜单授权信息(列表结构)", description = "有作用域限制")
    public Flux<MenuView> getScopeGrantInfo(@PathVariable String targetType,
                                            @PathVariable String targetId,
                                            @PathVariable String menuScope) {
        return getGrantInfo(targetType, targetId, MenuScope.valueOf(menuScope));
    }

    private Flux<MenuView> getGrantInfo(String targetType,
                                        String targetId,
                                        MenuScope menuScope) {

        //菜单
        final Mono<Map<String, MenuEntity>> mapMenuMono = defaultMenuService
            .getValidQuery(menuScope)
            .fetch()
            .collectMap(MenuEntity::getId, Function.identity());

        //菜单授权设置
        final Mono<Map<String, Collection<MenuGrantEntity>>> mapMenuGrantMono = menuGrantService
            .createQuery()
            .where(MenuGrantEntity::getDimensionTypeId, targetType)
            .and(MenuGrantEntity::getDimensionId, targetId)
            .fetch()
            .collectMultimap(MenuGrantEntity::getMenuId, Function.identity());

        return Mono.zip(settingService.getSettingDetail(targetType, targetId), mapMenuGrantMono, mapMenuMono)
            .flatMap(it -> {

                final AuthorizationSettingDetail detail = it.getT1();
                final Map<String, Collection<MenuGrantEntity>> menuGrantEntityMap = it.getT2();
                final Map<String, MenuEntity> menuEntityMap = it.getT3();

                return Mono.just(defaultMenuService.convertMenuView(menuEntityMap,
                    menuGrantEntityMap,
                    menu -> menu.hasPermission(detail::hasPermission),
                    button -> button.hasPermission(detail::hasPermission)
                ));
            }).flatMapIterable(Function.identity());
    }
}
