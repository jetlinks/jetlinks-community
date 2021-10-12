package org.jetlinks.community.auth.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuGrantRequest {


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
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType(targetType);
        detail.setTargetId(targetId);
        detail.setMerge(merge);
        detail.setPriority(priority);

        List<AuthorizationSettingDetail.PermissionInfo> permissionInfos = new ArrayList<>();

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
                        permissionInfos.add(AuthorizationSettingDetail.PermissionInfo.of(permission.getPermission(), permission.getActions()));
                    }
                }

                if (CollectionUtils.isNotEmpty(menuView.getButtons())) {
                    for (MenuView.ButtonView button : menuView.getButtons()) {
                        entity.getButton(button.getId())
                              .ifPresent(buttonInfo -> {
                                  if (CollectionUtils.isNotEmpty(buttonInfo.getPermissions())) {
                                      for (PermissionInfo permission : buttonInfo.getPermissions()) {
                                          permissionInfos
                                              .add(AuthorizationSettingDetail.PermissionInfo.of(permission.getPermission(), permission.getActions()));
                                      }

                                  }
                              });
                    }
                }
            }
        }
        detail.setPermissionList(permissionInfos);

        return detail;
    }


}
