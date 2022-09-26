package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
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

    public static OrganizationInfo of(Dimension dimension) {
        OrganizationInfo info = new OrganizationInfo();
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
}
