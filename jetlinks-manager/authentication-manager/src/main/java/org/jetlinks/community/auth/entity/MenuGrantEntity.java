package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.auth.enums.MenuScope;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 菜单授权定义实体类
 *
 * @author zeje
 */
@Getter
@Setter
@Table(name = "s_menu_grant", indexes = {
    @Index(name = "idx_menu_grant_ids", columnList = "dimension_type_id,dimension_id,menu_id")
})
public class MenuGrantEntity extends GenericEntity<String> {

    @Comment("维度类型ID")
    @Column(length = 32)
    @Schema(
        description = "维度类型ID"
    )
    private String dimensionTypeId;

    @Comment("维度ID")
    @Column(
        length = 64,
        updatable = false
    )
    @Schema(
        description = "维度ID"
    )
    private String dimensionId;

    @Column(
        length = 64,
        updatable = false
    )
    @Schema(
        description = "菜单Id"
    )
    private String menuId;

    @Comment("菜单作用域")
    @Column(name = "menu_scope", length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(
        description = "菜单作用域"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private MenuScope menuScope;

    @Column(
        length = 64,
        updatable = false
    )
    @Schema(
        description = "菜单下按钮Id"
    )
    private String buttonId;
}
