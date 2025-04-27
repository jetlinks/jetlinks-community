package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.i18n.SingleI18nSupportEntity;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiPredicate;

@Getter
@Setter
public class MenuButtonInfo implements SingleI18nSupportEntity, Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "按钮ID")
    private String id;

    @Schema(description = "按钮名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "权限信息")
    private List<PermissionInfo> permissions;

    @Schema(description = "其他配置")
    private Map<String, Object> options;

    @Schema(description = "i18n配置")
    private Map<String, String> i18nMessages;

    public String getI18nName() {
        if (MapUtils.isEmpty(i18nMessages)) {
            return name;
        }
        return getI18nMessage("name", name);
    }

    public boolean hasPermission(BiPredicate<String, Collection<String>> predicate) {
        if (CollectionUtils.isEmpty(permissions)) {
            return true;
        }

        for (PermissionInfo permission : permissions) {
            if (!predicate.test(permission.getPermission(), permission.getActions())) {
                return false;
            }
        }
        return true;
    }

    public static MenuButtonInfo of(String id, String name, String permission, String... actions) {
        MenuButtonInfo info = new MenuButtonInfo();
        info.setId(id);
        info.setName(name);
        info.setPermissions(Arrays.asList(PermissionInfo.of(permission, new HashSet<>(Arrays.asList(actions)))));
        return info;
    }
}
