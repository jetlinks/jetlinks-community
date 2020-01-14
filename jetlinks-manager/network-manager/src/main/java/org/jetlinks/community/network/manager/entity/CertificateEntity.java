package org.jetlinks.community.network.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.jetlinks.community.network.manager.enums.CertificateType;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

/**
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "certificate_info")
public class CertificateEntity extends GenericEntity<String> {

    @Comment("名称")
    @Column
    private String name;


    @Comment("类型")
    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    private CertificateType instance;

    @Comment("证书详情")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String, String> configs;

    @Comment("描述")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String description;
}
