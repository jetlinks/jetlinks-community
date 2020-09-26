package org.jetlinks.community.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "证书名称")
    private String name;

    @Comment("类型")
    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "证书类型")
    private CertificateType instance;

    @Comment("证书详情")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "证书配置")
    private CertificateConfig configs;

    @Comment("描述")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "说明")
    private String description;

    @Getter
    @Setter
    public static class CertificateConfig {

        @Schema(description = "证书内容(base64)")
        private String keystoreBase64;

        @Schema(description = "信任库内容(base64)")
        private String trustKeyStoreBase64;

        @Schema(description = "证书密码")
        private String keystorePwd;

        @Schema(description = "信任库密码")
        private String trustKeyStorePwd;
    }
}
