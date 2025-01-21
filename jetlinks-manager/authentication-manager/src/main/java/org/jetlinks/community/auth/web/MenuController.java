package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.*;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.jetlinks.community.auth.configuration.MenuProperties;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.service.MenuGrantService;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.web.response.ValidationResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 菜单管理
 *
 * @author zhouhao
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

    private final MenuGrantService grantService;

    private final MenuProperties properties;

    private final DefaultPermissionService permissionService;

    @Override
    public ReactiveCrudService<MenuEntity, String> getService() {
        return defaultMenuService;
    }


    /**
     * 获取获取全部菜单信息（树结构）
     *
     * @return 菜单列表
     */
    @QueryAction
    @PostMapping("/_all/tree")
    @Operation(summary = "获取获取全部菜单信息（树结构）")
    public Flux<MenuEntity> getAllMenuAsTree(@RequestBody Mono<QueryParamEntity> queryMono) {
        return queryMono
            .doOnNext(query -> query
                .toQuery()
                .orderByAsc(MenuEntity::getSortIndex)
                .noPaging())
            .flatMapMany(defaultMenuService::query)
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, MenuEntity::setChildren));
    }

    /**
     * 获取用户自己的菜单列表
     *
     * @return 菜单列表
     */
    @PostMapping("/user-own/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取当前用户可访问的菜单(树结构)")
    public Flux<MenuView> getUserMenuAsTree(@RequestBody(required = false) Mono<QueryParamEntity> queryMono) {
        return queryMono
            .flatMapMany(queryParam -> getUserMenuAsList(queryParam)
                .as(MenuController::listToTree));
    }


    /**
     * 获取用户自己的菜单列表
     *
     * @return 菜单列表
     */
    @GetMapping("/user-own/tree")
    @Authorize(merge = false)
    @QueryNoPagingOperation(summary = "获取当前用户可访问的菜单(树结构)")
    public Flux<MenuView> getUserMenuAsTree(QueryParamEntity queryParam) {
        return this
            .getUserMenuAsList(queryParam)
            .as(MenuController::listToTree);
    }

    @GetMapping("/user-own/list")
    @Authorize(merge = false)
    @QueryNoPagingOperation(summary = "获取当前用户可访问的菜单(列表结构)")
    public Flux<MenuView> getUserMenuAsList(QueryParamEntity queryParam) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(autz -> properties.isAllowAllMenu(autz)
                ?
                defaultMenuService
                    .getMenuViews(queryParam, menu -> true)
                    .doOnNext(MenuView::grantAll)
                :
                defaultMenuService.getGrantedMenus(queryParam, autz.getDimensions())
            );
    }

    /**
     * 获取菜单所属系统
     * 用于新增应用管理时，根据所属系统（owner）查询菜单信息
     *
     * @return 所属系统owner
     */
    @Authorize(ignore = true)
    @PostMapping("/owner")
    @Operation(summary = "获取菜单所属系统")
    public Flux<String> getSystemMenuOwner(@RequestBody @Parameter(description = "需要去除的所属系统，例如当前系统")
                                           Mono<List<String>> excludes) {
        return excludes.flatMapMany(owner -> defaultMenuService
                           .createQuery()
                           .and(MenuEntity::getOwner, TermType.nin, owner)
                           .fetch())
                       .map(MenuEntity::getOwner)
                       .distinct();
    }

    /**
     * 获取本系统菜单信息（树结构）
     * 用于应用管理之间同步菜单
     *
     * @return 菜单列表
     */
    @Authorize(ignore = true)
    @PostMapping("/owner/tree/{owner}")
    @Operation(summary = "获取本系统菜单信息（树结构）")
    public Flux<MenuEntity> getSystemMenuAsTree(@PathVariable @Parameter(description = "菜单所属系统") String owner,
                                                @RequestBody Mono<QueryParamEntity> queryMono) {
        return queryMono
            .doOnNext(query -> query
                .toQuery()
                .and(MenuEntity::getOwner, owner)
                .orderByAsc(MenuEntity::getSortIndex)
                .noPaging())
            .flatMapMany(defaultMenuService::query)
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, MenuEntity::setChildren));
    }

    @PutMapping("/{targetType}/{targetId}/_grant")
    @Operation(summary = "对菜单进行授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> grant(@PathVariable String targetType,
                            @PathVariable String targetId,
                            @RequestBody Mono<MenuGrantRequest> body) {
        //todo 防越权控制
        return body
            .doOnNext(request -> {
                request.setTargetType(targetType);
                request.setTargetId(targetId);
            })
            .flatMap(grantService::grant);
    }

    @PutMapping("/{targetType}/{targetId}/{owner}/clear-grant")
    @Operation(summary = "清空菜单授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> cleargrant(@PathVariable String targetType,
                                 @PathVariable String targetId,
                                 @PathVariable String owner) {
        return grantService.clearGrant(targetType, targetId, Collections.singleton(owner));
    }

    @PutMapping("/_batch/_grant")
    @Operation(summary = "对菜单批量进行授权")
    @ResourceAction(id = "grant", name = "授权")
    public Mono<Void> grant(@RequestBody Flux<MenuGrantRequest> body) {
        return body
            .flatMap(grantService::grant)
            .then();
    }

    @GetMapping("/{targetType}/{targetId}/_grant/tree")
    @ResourceAction(id = "grant", name = "授权")
    @QueryNoPagingOperation(summary = "获取菜单授权信息(树结构)")
    public Flux<MenuView> getGrantInfoTree(@PathVariable String targetType,
                                           @PathVariable String targetId,
                                           QueryParamEntity query) {

        return this
            .getGrantInfo(targetType, targetId, query)
            .as(MenuController::listToTree);
    }

    @GetMapping("/{targetType}/{targetId}/_grant/list")
    @ResourceAction(id = "grant", name = "授权")
    @QueryNoPagingOperation(summary = "获取菜单授权信息(列表结构)")
    public Flux<MenuView> getGrantInfo(@PathVariable String targetType,
                                       @PathVariable String targetId,
                                       QueryParamEntity query) {

        Flux<MenuView> allMenu = this.getUserMenuAsList(query).cache();
        return Mono
            .zip(
                defaultMenuService
                    .getGrantedMenus(targetType, targetId)
                    .collectMap(GenericEntity::getId),
                allMenu.collectMap(MenuView::getId, Function.identity()),
                (granted, all) -> LocaleUtils
                    .currentReactive()
                    .flatMapMany(locale -> allMenu
                        .doOnNext(MenuView::resetGrant)
                        .map(view -> view
                            .withGranted(granted.get(view.getId()))
                        )))
            .flatMapMany(Function.identity());

    }


    @PostMapping("/permissions")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "根据菜单获取对应的权限")
    public Flux<AuthorizationSettingDetail.PermissionInfo> getPermissionsByMenuGrant(@RequestBody Flux<MenuView> menus) {
        return getAuthorizationSettingDetail(menus)
            .flatMapIterable(AuthorizationSettingDetail::getPermissionList);
    }


    @PostMapping("/asset-types")
    @ResourceAction(id = "grant", name = "授权")
    @Operation(summary = "根据菜单获取对应的资产类型")
    @Deprecated
    public Flux<AssetTypeView> getAssetTypeByMenuGrant(@RequestBody Flux<MenuView> menus) {
        // 社区版本目前不支持数据权限控制
        return Flux.empty();

    }

    @PatchMapping("/{owner}/_all")
    @SaveAction
    @Transactional
    @Operation(summary = "保存一个应用下的全量数据", description = "先应用下全部删除旧数据，再新增数据")
    public Mono<SaveResult> saveOwnerAll(@PathVariable String owner, @RequestBody Flux<MenuEntity> menus) {
        return this
            .getService()
            .createDelete()
            .where(MenuEntity::getStatus, 1)
            .and(MenuEntity::getOwner, owner)
            .execute()
            .then(
                this.save(menus)
            );
    }


    @GetMapping("/code/_validate")
    @QueryAction
    @Operation(summary = "验证菜单编码是否合法", description = "同一所有者的相同应用下的菜单，编码不能重复")
    public Mono<ValidationResult> codeValidate(@RequestParam @Parameter(description = "菜单编码") String code,
                                               @RequestParam(required = false)
                                               @Parameter(description = "外部菜单所属应用ID") String appId,
                                               @RequestParam @Parameter(description = "菜单所有者") String owner) {
        return LocaleUtils.currentReactive()
                          .flatMap(locale -> defaultMenuService
                              .createQuery()
                              .where(MenuEntity::getCode, code)
                              .and(MenuEntity::getOwner, owner)
                              .fetch()
                              .next()
                              .map(menu -> ValidationResult
                                  .error(LocaleUtils.resolveMessage("error.id_already_exists", locale))))
                          .defaultIfEmpty(ValidationResult.success())
                          .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                                                                             .map(ValidationResult::error));
    }

    private Mono<AuthorizationSettingDetail> getAuthorizationSettingDetail(Flux<MenuView> menus) {
        return Mono
            .zip(menus
                     .doOnNext(view -> {
                         view.setGranted(true);
                         if (null != view.getButtons()) {
                             for (MenuView.ButtonView button : view.getButtons()) {
                                 button.setGranted(true);
                             }
                         }
                     })
                     .collectList()
                     .map(list -> {
                         MenuGrantRequest request = new MenuGrantRequest();
                         request.setTargetType("temp");
                         request.setTargetId("temp");
                         request.setMenus(list);
                         return request;
                     }),
                 defaultMenuService
                     .createQuery()
                     .fetch()
                     .collectList(),
                 MenuGrantRequest::toAuthorizationSettingDetail
            );
    }

    private static Flux<MenuView> listToTree(Flux<MenuView> flux) {
        return flux
            .collectList()
            .flatMapIterable(list -> TreeUtils.list2tree(list, MenuView::getId, MenuView::getParentId, MenuView::setChildren));
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class AssetTypeView {
        private String id;
        private String name;
    }

}
