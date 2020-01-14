package org.jetlinks.community.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

/**
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

    @Comment("菜单名称")
    @Column(length = 32)
    private String name;

    @Comment("描述")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String describe;

    @Comment("权限表达式")
    @Column(name = "permission_expression", length = 256)
    private String permissionExpression;

    @Comment("菜单对应的url")
    @Column(length = 512)
    private String url;

    @Comment("图标")
    @Column(length = 256)
    private String icon;

    @Comment("状态")
    @Column
    @ColumnType(jdbcType = JDBCType.SMALLINT)
    private Byte status;
    //子菜单
    private List<MenuEntity> children;

    @Override
    public List<MenuEntity> getChildren() {
        return children;
    }
}
