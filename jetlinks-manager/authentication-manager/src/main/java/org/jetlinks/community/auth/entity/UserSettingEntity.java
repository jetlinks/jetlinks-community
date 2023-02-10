package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "s_user_settings", indexes = {
    @Index(name = "idx_user_id_type", columnList = "user_id,type")
})
@Comment("用户设置信息")
public class UserSettingEntity extends GenericEntity<String> {

    @Schema(description = "用户ID", hidden = true)
    @Column(length = 64, nullable = false, updatable = false)
    @Length(max = 64, groups = CreateGroup.class)
    private String userId;

    @Schema(description = "配置类型,如: search")
    @Column(length = 64, nullable = false, updatable = false)
    @Length(max = 64, groups = CreateGroup.class)
    private String type;

    @Schema(description = "配置key,如: user-search")
    @Column(length = 64, nullable = false, updatable = false)
    @Length(max = 64, groups = CreateGroup.class)
    private String key;

    @Schema(description = "配置名称")
    @Column(length = 128)
    @Length(max = 128)
    private String name;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "配置内容")
    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private String content;

    @Column(updatable = false, nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;

    @Override
    public String getId() {
        if (!StringUtils.hasText(super.getId())) {
            generateId();
        }
        return super.getId();
    }

    public String generateId() {
        String id;
        setId(id = generateId(userId, type, key));
        return id;
    }

    public static String generateId(String userId, String type, String key) {
        return DigestUtils.md5Hex(String.join("|", userId, type, key));
    }
}
