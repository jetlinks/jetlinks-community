package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Dimension;
import org.jetlinks.reactor.ql.utils.CastUtils;

@Getter
@Setter
public class OrganizationInfo {

    @Schema(description = "机构(部门ID)")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "上级ID")
    private String parentId;

    @Schema(description = "序号")
    private long sortIndex;

    @Schema(description = "组织机构完整名称")
    private String fullName;

    public static final String INTERVAL_CHARACTER = "/";

    public static OrganizationInfo of() {
        return EntityFactoryHolder.newInstance(OrganizationInfo.class, OrganizationInfo::new);
    }

    public static OrganizationInfo from(OrganizationEntity entity) {
        return entity.copyTo(of());
    }


    public OrganizationInfo with(Dimension dimension) {
        OrganizationInfo info = this;
        info.setId(dimension.getId());
        info.setName(dimension.getName());

        dimension.getOption("parentId")
                 .map(String::valueOf)
                 .ifPresent(info::setParentId);

        dimension.getOption("code")
                 .map(String::valueOf)
                 .ifPresent(info::setCode);
        dimension.getOption("sortIndex")
                 .map(sortIndex -> CastUtils.castNumber(sortIndex).longValue())
                 .ifPresent(info::setSortIndex);
        return info;
    }

    /**
     * 添加上一级和本级名称，以/分隔
     *
     * @param parentName 父级名称
     */
    public void addParentFullName(String parentName) {
        String fullName = this.fullName == null ? this.getName() : this.fullName;
        if (StringUtils.isNotBlank(parentName)) {
            this.setFullName(parentName + INTERVAL_CHARACTER + fullName);
        } else {
            this.setFullName(fullName);
        }
    }

    public String getFullName() {
        return fullName == null ? name : fullName;
    }

    public static OrganizationInfo of(Dimension dimension) {
        return of().with(dimension);
    }
}
