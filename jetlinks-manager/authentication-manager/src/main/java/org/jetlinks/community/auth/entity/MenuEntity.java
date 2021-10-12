package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单定义实体类
 *
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "s_menu", indexes = {
    @Index(name = "idx_menu_path", columnList = "path")
})
public class MenuEntity
    extends GenericTreeSortSupportEntity<String> {

    @Schema(description = "名称")
    @Comment("菜单名称")
    @Column(length = 32)
    private String name;

    @Comment("描述")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "描述")
    private String describe;

    @Hidden
    @Deprecated
    @Comment("权限表达式")
    @Column(name = "permission_expression", length = 256)
    private String permissionExpression;

    @Comment("菜单对应的url")
    @Column(length = 512)
    @Schema(description = "URL,路由")
    private String url;

    @Comment("图标")
    @Column(length = 256)
    @Schema(description = "图标")
    private String icon;

    @Comment("状态")
    @Column
    @ColumnType(jdbcType = JDBCType.SMALLINT)
    @Schema(description = "状态,0为禁用,1为启用")
    @DefaultValue("1")
    private Byte status;

    @Schema(description = "默认权限信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private List<PermissionInfo> permissions;

    @Schema(description = "按钮定义信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private List<MenuButtonInfo> buttons;

    @Schema(description = "其他配置信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Object> options;

    //子菜单
    @Schema(description = "子菜单")
    private List<MenuEntity> children;

    public MenuEntity copy(Predicate<MenuButtonInfo> buttonPredicate) {
        MenuEntity entity = this.copyTo(new MenuEntity());

        if (CollectionUtils.isEmpty(entity.getButtons())) {
            return entity;
        }
        entity.setButtons(
            entity
                .getButtons()
                .stream()
                .filter(buttonPredicate)
                .collect(Collectors.toList())
        );
        return entity;
    }

    public boolean hasPermission(BiPredicate<String, Collection<String>> predicate) {
        if (CollectionUtils.isEmpty(permissions) && CollectionUtils.isEmpty(buttons)) {
            return false;
        }
        //有权限信息
        if (CollectionUtils.isNotEmpty(permissions)) {
            for (PermissionInfo permission : permissions) {
                if (!predicate.test(permission.getPermission(), permission.getActions())) {
                    return false;
                }
            }
            return true;
        }
        //有任意按钮信息
        if (CollectionUtils.isNotEmpty(buttons)) {
            for (MenuButtonInfo button : buttons) {
                if (button.hasPermission(predicate)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<MenuButtonInfo> getButton(String id) {
        if (buttons == null) {
            return Optional.empty();
        }
        return buttons
            .stream()
            .filter(button -> Objects.equals(button.getId(), id))
            .findAny();
    }
}
