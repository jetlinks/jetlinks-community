package org.jetlinks.community.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.DefaultExtendable;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Dimension;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class PositionDetail extends DefaultExtendable {

    @Schema(description = "ID")
    private String id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "组织ID")
    private String orgId;
    @Schema(description = "组织名称")
    private String orgName;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "上级ID")
    private String parentId;
    @Schema(description = "上级职位名称")
    private String parentName;
    @Schema(description = "排序序号")
    private long sortIndex;
    @Schema(description = "职位成员数量")
    private int memberCount;
    @Schema(description = "全名")
    private String fullName;

    private List<PositionDetail> children;

    private List<RoleInfo> roles;

    public PositionEntity toEntity() {
        return PositionEntity.of().copyFrom(this);
    }

    public void withOrg(OrganizationInfo info) {
        if (info == null) {
            return;
        }
        this.setFullName(info.getFullName() + "/" + this.getName());
    }

    public String getFullName() {
        return fullName == null ? name : fullName;
    }

    public static PositionDetail of() {
        return EntityFactoryHolder.newInstance(PositionDetail.class, PositionDetail::new);
    }

    public static PositionDetail from(PositionEntity position) {
        return position.copyTo(of());
    }

    public static PositionDetail of(Dimension dimension) {
        PositionDetail detail = of();
        detail.setId(dimension.getId());
        detail.setName(dimension.getName());

        dimension
            .getOption(PositionEntity.DIMENSION_OPTION_PARENT_ID)
            .map(String::valueOf)
            .ifPresent(detail::setParentId);

        dimension
            .getOption(PositionEntity.DIMENSION_OPTION_CODE)
            .map(String::valueOf)
            .ifPresent(detail::setCode);

        dimension
            .getOption(PositionEntity.DIMENSION_OPTION_ORG_ID)
            .map(String::valueOf)
            .ifPresent(detail::setOrgId);

        return detail;
    }


    @JsonIgnore
    public List<String> getRoleIdList() {
        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptyList();
        }
        return roles
            .stream()
            .map(RoleInfo::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public PositionDetail withRoles(List<RoleEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return this;
        }
        this.roles = entities.stream()
                             .map(RoleInfo::of)
                             .collect(Collectors.toList());
        return this;
    }
}
