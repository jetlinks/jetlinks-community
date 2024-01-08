package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@Table(name = "s_role_group")
@Comment("角色分组表")
@EnableEntityEvent
public class RoleGroupEntity extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity {

    @Column(length = 64)
    @Length(min = 1, max = 64)
    @Schema(description = "名称")
    private String name;

    @Column
    @Length(max = 255)
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
    @Schema(description = "创建时间"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    private List<RoleGroupEntity> children;
}
