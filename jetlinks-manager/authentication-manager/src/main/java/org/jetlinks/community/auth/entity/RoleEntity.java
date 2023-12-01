package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.community.auth.enums.RoleState;
import org.jetlinks.community.auth.service.RoleGroupService;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "s_role")
@Comment("角色信息表")
public class RoleEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Column(length = 64)
    @Length(min = 1, max = 64)
    @Schema(description = "名称")
    private String name;

    @Column
    @Length(max = 255)
    @Schema(description = "说明")
    private String description;

    @Column(length = 32)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "状态。enabled为正常，disabled为已禁用")
    @DefaultValue("enabled")
    private RoleState state;

    @Column(length = 64)
    @Schema(description = "所属分组")
    @DefaultValue(RoleGroupService.DEFAULT_GROUP_ID)
    private String groupId;

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

    public Dimension toDimension() {
        SimpleDimension dimension = new SimpleDimension();
        dimension.setId(getId());
        dimension.setName(name);
        dimension.setType(DefaultDimensionType.role);
        return dimension;
    }
}
