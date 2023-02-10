package org.jetlinks.community.auth.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuGrantRequest {

    @Schema(description = "权限类型,如: org,openApi")
    private String targetType;

    @Schema(description = "权限类型对应的数据ID")
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
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType(targetType);
        detail.setTargetId(targetId);
        detail.setMerge(merge);
        detail.setPriority(priority);

        Map<String, Set<String>> permissionInfos = new ConcurrentHashMap<>();

        for (MenuView menu : menus) {
            //平铺
            List<MenuView> expand = TreeSupportEntity.expandTree2List(menu, IDGenerator.MD5);
            for (MenuView menuView : expand) {
                if (!menu.isGranted()) {
                    continue;
                }
                MenuEntity entity = menuMap.get(menuView.getId());
                if (entity == null) {
                    continue;
                }
                //自动持有配置的权限
                if (CollectionUtils.isNotEmpty(entity.getPermissions())) {
                    for (PermissionInfo permission : entity.getPermissions()) {
                        if (StringUtils.hasText(permission.getPermission()) && CollectionUtils.isNotEmpty(permission.getActions())) {
                            permissionInfos
                                .computeIfAbsent(permission.getPermission(), ignore -> new HashSet<>())
                                .addAll(permission.getActions());
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(menuView.getButtons())) {
                    for (MenuView.ButtonView button : menuView.getButtons()) {
                        if (!button.isGranted()) {
                            continue;
                        }
                        entity.getButton(button.getId())
                              .ifPresent(buttonInfo -> {
                                  if (CollectionUtils.isNotEmpty(buttonInfo.getPermissions())) {
                                      for (PermissionInfo permission : buttonInfo.getPermissions()) {
                                          if (CollectionUtils.isEmpty(permission.getActions())) {
                                              continue;
                                          }
                                          permissionInfos
                                              .computeIfAbsent(permission.getPermission(), ignore -> new HashSet<>())
                                              .addAll(permission.getActions());
                                      }

                                  }
                              });
                    }
                }
            }
        }
        detail.setPermissionList(permissionInfos
                                     .entrySet()
                                     .stream()
                                     .map(e -> AuthorizationSettingDetail.PermissionInfo.of(e.getKey(), e.getValue()))
                                     .collect(Collectors.toList()));

        return detail;
    }


    public List<MenuBindEntity> toBindEntities() {
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }
        List<MenuView> entities = new ArrayList<>();
        for (MenuView menu : menus) {
            TreeSupportEntity.expandTree2List(menu, entities, IDGenerator.MD5);
        }
        return entities
            .stream()
            .filter(MenuView::isGranted)
            .map(menu -> MenuBindEntity
                .of(menu)
                .withTarget(targetType, targetId)
                .withMerge(merge, priority))
            .collect(Collectors.toList());

    }
}
