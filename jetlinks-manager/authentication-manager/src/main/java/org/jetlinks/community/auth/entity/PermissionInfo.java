package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class PermissionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限ID")
    private String permission;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限操作")
    private Set<String> actions;

    public static PermissionInfo of(String permission, Set<String> actions) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setPermission(permission);
        permissionInfo.setActions(actions);
        return permissionInfo;
    }

}
