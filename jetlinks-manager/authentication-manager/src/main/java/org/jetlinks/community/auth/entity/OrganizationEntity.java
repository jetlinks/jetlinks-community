package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.auth.dimension.OrgDimensionType;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Table(name = "s_organization")
@Comment("机构信息表")
@EnableEntityEvent
public class OrganizationEntity extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity {

    @Override
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", message = "ID只能由数字,字母,下划线和中划线组成", groups = CreateGroup.class)
    @Schema(description = "机构ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }

    @Column
    @Schema(description = "编码")
    private String code;

    @Column
    @Schema(description = "名称")
    private String name;

    @Column
    @Schema(description = "类型")
    private String type;

    @Column
    @Schema(description = "说明")
    private String describe;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR)
    @JsonCodec
    @Schema(description = "其他配置")
    private Map<String, Object> properties;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    private List<OrganizationEntity> children;

    public Dimension toDimension(boolean direct) {
        Map<String, Object> options = new HashMap<>();
        options.put("direct", direct);
        return SimpleDimension.of(getId(), getName(), OrgDimensionType.org, options);
    }
}
