package org.jetlinks.community.network.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.network.manager.enums.CertificateFormat;
import org.jetlinks.community.network.manager.enums.CertificateMode;
import org.jetlinks.community.network.manager.enums.CertificateType;
import org.jetlinks.community.network.security.DefaultCertificate;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;

/**
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "certificate_info")
@Comment("证书信息表")
@EnableEntityEvent
public class CertificateEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column
    @Schema(description = "证书名称")
    private String name;

    @Column(length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "证书类型")
    @DefaultValue("common")
    @NotNull(groups = CreateGroup.class)
    private CertificateType type;

    @Column(length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "证书格式")
    @DefaultValue("PEM")
    @NotNull(groups = CreateGroup.class)
    private CertificateFormat format;

    @Column(length = 16)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "证书模式,Server or Client")
    @DefaultValue("server")
    @NotNull(groups = CreateGroup.class)
    private CertificateMode mode;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "证书配置")
    @NotNull(groups = CreateGroup.class)
    private CertificateConfig configs;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "说明")
    private String description;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Getter
    @Setter
    public static class CertificateConfig {

        @Schema(description = "PEM:私钥内容")
        private String key;

        @Schema(description = "PEM:证书内容")
        private String cert;

        @Schema(description = "PEM:信任证书,用于客户端模式")
        private String trust;

        //JKS 或者 PFX 才有效
        @Schema(description = "证书库内容(base64)", hidden = true)
        private String keystoreBase64;

        //JKS 或者 PFX 才有效
        @Schema(description = "信任库内容(base64)", hidden = true)
        private String trustKeyStoreBase64;

        //JKS 或者 PFX 才有效
        @Schema(description = "证书密码", hidden = true)
        private String keystorePwd;

        //JKS 或者 PFX 才有效
        @Schema(description = "信任库密码", hidden = true)
        private String trustKeyStorePwd;
    }

    @Override
    public void tryValidate(Class<?>... groups) {
        super.tryValidate(groups);
        //新增时校验证书
        if (groups.length == 0 || groups[0] == CreateGroup.class) {
            validate();
        }
    }

    public void validate() {
        Assert.notNull(configs, "error.cert_configs_can_not_be_null");
        if (format == CertificateFormat.PEM) {
            if (mode == CertificateMode.server) {
                Assert.hasText(configs.getKey(), "error.pem_key_can_not_be_empty");
                Assert.hasText(configs.getCert(), "error.pem_cert_can_not_be_empty");
            } else if (mode == CertificateMode.client) {
                Assert.hasText(configs.getTrust(), "error.pem_trust_can_not_be_empty");
            }
        }
        format.init(new DefaultCertificate(getId(), getName()), configs);
    }
}
