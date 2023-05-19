package org.jetlinks.community.config.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Table(name = "s_config", indexes = {
    @Index(name = "idx_conf_scope", columnList = "scope")
})
@Getter
@Setter
@EnableEntityEvent
public class ConfigEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "作用域")
    private String scope;

    @Column(nullable = false)
    @Schema
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    private Map<String,Object> properties;

    @Override
    public String getId() {
        if (!StringUtils.hasText(super.getId())) {
            setId(generateId(scope));
        }
        return super.getId();
    }

    public static String generateId(String scope) {
        return DigestUtils.md5Hex(String.join("|", scope));
    }
}
