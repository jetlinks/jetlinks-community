package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Map;

/**
 * 第三方用户绑定实体
 *
 * @author zhouhao
 * @since 1.5
 */
@Getter
@Setter
@Table(name = "s_third_party_user_bind", indexes = {
    @Index(name = "idx_thpub_user_id", columnList = "user_id"),
    @Index(name = "idx_thpub_tpu_tpu", columnList = "type,provider,user_id")
})
@Comment("第三方用户绑定信息表")
@EnableEntityEvent
public class ThirdPartyUserBindEntity extends GenericEntity<String> {

    @Schema(description = "绑定类型,如: 微信,钉钉")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String type;

    @Schema(description = "第三方标识,如: 微信企业A,企业B")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String provider;

    @Schema(description = "第三方名称")
    @Column(nullable = false, length = 64)
    private String providerName;

    @Schema(description = "第三方用户ID")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String thirdPartyUserId;

    @Schema(description = "平台用户ID")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String userId;

    @Schema(description = "绑定时间")
    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long bindTime;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "其他配置信息")
    private Map<String, Object> others;

    public void generateId() {
        setId(generateId(type, provider, thirdPartyUserId));
    }

    public static String generateId(String... arr) {
        return DigestUtils.md5Hex(String.join("|", arr));
    }
}
