package org.jetlinks.community.auth.web.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuGrantEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.jetlinks.community.auth.enums.MenuScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuGrantRequest {

    private MenuScope menuScope;

    private String targetType;

    private String targetId;

    /**
     * 冲突时是否合并
     */
    @Schema(description = "冲突时是否合并")
    private boolean merge = true;

    /**
     * 冲突时优先级
     */
    @Schema(description = "冲突时合并优先级")
    private int priority = 10;

    @Schema(description = "授权的菜单信息")
    private List<MenuView> menus;

    public AuthorizationSettingDetail toAuthorizationSettingDetail(List<MenuEntity> menuEntities) {
        Map<String, MenuEntity> menuMap = menuEntities
            .stream()
            .collect(Collectors.toMap(MenuEntity::getId, Function.identity()));
        return toAuthorizationSettingDetail(menuMap);
    }

    private AuthorizationSettingDetail toAuthorizationSettingDetail(Map<String, MenuEntity> menuMap) {

        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType(targetType);
        detail.setTargetId(targetId);
        detail.setMerge(merge);
        detail.setPriority(priority);

        Map<String, AuthorizationSettingDetail.PermissionInfo> permissionInfoMap = new HashMap<>();
        //List<AuthorizationSettingDetail.PermissionInfo> permissionInfos = new ArrayList<>();

        for (MenuView menu : menus) {
            //平铺
            List<MenuView> expand = TreeSupportEntity.expandTree2List(menu, IDGenerator.MD5);
            for (MenuView menuView : expand) {
                MenuEntity entity = menuMap.get(menuView.getId());
                if (entity == null) {
                    continue;
                }
                //自动持有配置的权限
                if (CollectionUtils.isNotEmpty(entity.getPermissions())) {
                    for (PermissionInfo permission : entity.getPermissions()) {
                        putPermission(permissionInfoMap, permission);
                    }
                }

                if (CollectionUtils.isNotEmpty(menuView.getButtons())) {
                    for (MenuView.ButtonView button : menuView.getButtons()) {
                        entity.getButton(button.getId())
                            .ifPresent(buttonInfo -> {
                                if (CollectionUtils.isNotEmpty(buttonInfo.getPermissions())) {
                                    for (PermissionInfo permission : buttonInfo.getPermissions()) {
                                        putPermission(permissionInfoMap, permission);
                                    }
                                }
                            });
                    }
                }
            }
        }

        detail.setPermissionList(permissionInfoMap.values().stream().collect(Collectors.toList()));

        return detail;
    }

    /**
     * @param permissionInfoMap
     * @param permission
     */
    private void putPermission(Map<String, AuthorizationSettingDetail.PermissionInfo> permissionInfoMap, PermissionInfo permission) {
        if (permissionInfoMap.containsKey(permission.getPermission())) {
            AuthorizationSettingDetail.PermissionInfo permissionInfo = permissionInfoMap.get(permission.getPermission());
            permissionInfo.setActions(Sets.union(permissionInfo.getActions(), permission.getActions()));
        } else {
            permissionInfoMap.put(permission.getPermission(), AuthorizationSettingDetail.PermissionInfo.of(permission.getPermission(), permission.getActions()));
        }
    }

    public List<MenuGrantEntity> toMenuGrantEntities(List<MenuEntity> menuEntities) {
        Map<String, MenuEntity> menuMap = menuEntities
            .stream()
            .collect(Collectors.toMap(MenuEntity::getId, Function.identity()));
        return toMenuGrantEntities(menuMap);
    }

    public List<MenuGrantEntity> toMenuGrantEntities(Map<String, MenuEntity> menuMap) {
        List<MenuGrantEntity> menuGrantEntities = Lists.newArrayList();

        for (MenuView menu : menus) {
            //平铺
            List<MenuView> expand = TreeSupportEntity.expandTree2List(menu, IDGenerator.MD5);
            for (MenuView menuView : expand) {
                MenuEntity entity = menuMap.get(menuView.getId());
                if (entity == null) {
                    continue;
                }
                //自动持有配置的权限
                if (CollectionUtils.isNotEmpty(entity.getPermissions())) {
                    //若存在有配置的权限，代表能够进入界面
                    MenuGrantEntity menuGrantEntity = new MenuGrantEntity();
                    menuGrantEntity.setDimensionTypeId(targetType);
                    menuGrantEntity.setDimensionId(targetId);
                    menuGrantEntity.setMenuScope(menuScope);
                    menuGrantEntity.setMenuId(entity.getId());
                    menuGrantEntity.setButtonId(null);
                    menuGrantEntities.add(menuGrantEntity);
                }
                //按钮
                if (CollectionUtils.isNotEmpty(menuView.getButtons())) {
                    for (MenuView.ButtonView button : menuView.getButtons()) {
                        MenuGrantEntity menuGrantEntity = new MenuGrantEntity();
                        menuGrantEntity.setDimensionTypeId(targetType);
                        menuGrantEntity.setDimensionId(targetId);
                        menuGrantEntity.setMenuScope(menuScope);
                        menuGrantEntity.setMenuId(entity.getId());
                        menuGrantEntity.setButtonId(button.getId());
                        menuGrantEntities.add(menuGrantEntity);
                    }
                }
            }
        }

        return menuGrantEntities;
    }
}
