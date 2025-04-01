package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.simple.SimpleDimension;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.authorize.OrgDimensionType;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "职位")
@Getter
@Setter
@EnableEntityEvent
@Table(name = "s_org_position")
public class PositionEntity extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity {

    public static final String DIMENSION_OPTION_ORG_ID = "orgId";
    public static final String DIMENSION_OPTION_PARENT_ID = "parentId";
    public static final String DIMENSION_OPTION_CODE = "code";

    private static final long serialVersionUID = 1L;

    @Schema(description = "组织ID")
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String orgId;

    @Schema(description = "名称")
    @Column(length = 128, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    private String name;

    @Schema(description = "编码")
    @Column(length = 64)
    private String code;

    @Schema(description = "描述")
    @Column(length = 1024)
    private String description;

    @Column(length = 64, updatable = false)
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(updatable = false)
    @Schema(description = "创建时间")
    private Long createTime;

    private List<PositionEntity> children;

    public static PositionEntity of() {
        return EntityFactoryHolder
            .newInstance(PositionEntity.class, PositionEntity::new);
    }

    public Dimension toDimension(boolean direct) {
        Map<String, Object> options = new HashMap<>();
        options.put("direct", direct);
        options.put(DIMENSION_OPTION_PARENT_ID, getParentId());
        options.put(DIMENSION_OPTION_CODE, getCode());
        options.put(DIMENSION_OPTION_ORG_ID, getOrgId());
        return SimpleDimension.of(getId(), getName(), OrgDimensionType.position, options);
    }

    public Dimension toParentDimension() {
        Map<String, Object> options = new HashMap<>();
        options.put(DIMENSION_OPTION_PARENT_ID, getParentId());
        options.put(DIMENSION_OPTION_CODE, getCode());
        options.put(DIMENSION_OPTION_ORG_ID, getOrgId());
        return SimpleDimension.of(getId(), getName(), OrgDimensionType.parentPosition, options);
    }

}
