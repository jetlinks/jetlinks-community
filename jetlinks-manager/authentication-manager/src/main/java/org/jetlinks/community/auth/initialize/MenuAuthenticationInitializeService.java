package org.jetlinks.community.auth.initialize;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.events.AuthorizationInitializeEvent;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimplePermission;
import org.hswebframework.web.system.authorization.api.entity.ActionEntity;
import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class MenuAuthenticationInitializeService {

    private final DefaultMenuService menuService;

    private final DefaultPermissionService permissionService;

    /**
     * 根据角色配置的菜单权限来重构权限信息
     *
     * @param event 权限初始化事件
     */
    @EventListener
    public void refactorPermission(AuthorizationInitializeEvent event) {
        if (event.getAuthentication().getDimensions().isEmpty()) {
            return;
        }
        event.async(
            Mono
                .zip(
                    // T1: 权限定义列表
                    permissionService
                        .createQuery()
                        .where(PermissionEntity::getStatus, 1)
                        .fetch()
                        .collectMap(PermissionEntity::getId, Function.identity()),
                    // T2: 菜单定义列表
                    menuService
                        .createQuery()
                        .where(MenuEntity::getStatus, 1)
                        .fetch()
                        .collectList(),
                    // T3: 角色赋予的菜单列表
                    menuService
                        .getGrantedMenus(QueryParamEntity.of(), event
                            .getAuthentication()
                            .getDimensions())
                        .collectList()
                        .filter(CollectionUtils::isNotEmpty)
                )
                .<Permission>flatMapIterable(tp3 -> {
                    Map<String, PermissionEntity> permissions = tp3.getT1();
                    List<MenuEntity> menus = tp3.getT2();
                    List<MenuView> grantedMenus = tp3.getT3();
                    MenuGrantRequest request = new MenuGrantRequest();
                    request.setTargetType(DefaultDimensionType.role.getId());
                    request.setTargetId("merge");
                    request.setMenus(grantedMenus);
                    AuthorizationSettingDetail detail = request.toAuthorizationSettingDetail(menus);
                    return detail
                        .getPermissionList()
                        .stream()
                        .map(per -> {
                            PermissionEntity entity = permissions.get(per.getId());
                            if (entity == null || per.getActions() == null) {
                                return null;
                            }

                            Set<String> actions;
                            if (CollectionUtils.isEmpty(entity.getActions())) {
                                actions = new HashSet<>();
                            } else {
                                Set<String> defActions = entity
                                    .getActions()
                                    .stream()
                                    .map(ActionEntity::getAction)
                                    .collect(Collectors.toSet());
                                actions = new HashSet<>(per.getActions());
                                actions.retainAll(defActions);
                            }

                            return SimplePermission
                                .builder()
                                .id(entity.getId())
                                .name(entity.getName())
                                .options(entity.getProperties())
                                .actions(actions)
                                .build();
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                })
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .doOnNext(mapping -> {
                    SimpleAuthentication authentication = new SimpleAuthentication();
                    authentication.setUser(event.getAuthentication().getUser());
                    authentication.setPermissions(mapping);
                    event.setAuthentication(event.getAuthentication().merge(authentication));
                })
        );

    }

}
