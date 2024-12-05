package org.jetlinks.community.relation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_object_relation")
@Schema(description = "对象关系定义实体")
@EnableEntityEvent
public class RelationEntity extends GenericEntity<String> {

    @Schema(description = "对象类型")
    @Column(length = 32, nullable = false, updatable = false)
    private String objectType;

    @Schema(description = "对象名称")
    @Column(length = 64, nullable = false)
    private String objectTypeName;

    @Schema(description = "关系标识")
    @Column(length = 32, nullable = false, updatable = false)
    private String relation;

    @Schema(description = "关系名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Schema(description = "反转关系名称")
    @Column(length = 64)
    private String reverseName;

    @Schema(description = "目标对象类型")
    @Column(length = 32, nullable = false, updatable = false)
    private String targetType;

    @Schema(description = "目标对象名称")
    @Column(length = 64, nullable = false)
    private String targetTypeName;

    @Column(nullable = false, updatable = false)
    @Schema(description = "创建时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @Schema(description = "其他拓展信息")
    private Map<String, Object> expands;

    @Override
    public String getId() {
        if (StringUtils.hasText(super.getId())) {
            return super.getId();
        }
        if (StringUtils.hasText(objectType) &&
            StringUtils.hasText(relation) &&
            StringUtils.hasText(targetType)) {
            generateId();
        }
        return super.getId();
    }

    public void generateId() {
        setId(
            DigestUtils.md5Hex(
                String.join("|", objectType, relation, targetType)
            )
        );
    }

    public RelationEntity from(String type, String name) {
        this.objectType = type;
        this.objectTypeName = name;
        return this;
    }

    public RelationEntity to(String type, String name) {
        this.targetType = type;
        this.targetTypeName = name;
        return this;
    }

    public RelationEntity relation(String relation, String name) {
        this.relation = relation;
        this.name = name;
        return this;
    }

}
