package org.jetlinks.community.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.jetlinks.community.auth.dimension.OrgDimensionType;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.enums.UserEntityType;
import org.jetlinks.community.auth.enums.UserEntityTypes;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class UserDetail {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(hidden = true)
    private String password;

    @Schema(hidden = true)
    private UserEntityType type;

    @Schema(description = "用户类型ID")
    private String typeId;

    @Schema(description = "用户状态。1启用，0禁用")
    private Byte status;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "email")
    private String email;

    @Schema(description = "联系电话")
    private String telephone;

    @Schema(description = "头像图片地址")
    private String avatar;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "创建时间")
    private long createTime;

    @Schema(description = "角色信息")
    private List<RoleInfo> roleList;

    @Schema(description = "所在机构(部门)信息")
    private List<OrganizationInfo> orgList;

    @Schema(description = "创建者ID")
    private String creatorId;

    private boolean tenantDisabled;

    public static UserDetail of(UserEntity entity) {
        return new UserDetail().with(entity);
    }

    public UserDetail with(UserDetailEntity entity) {
        this.setAvatar(entity.getAvatar());
        this.setDescription(entity.getDescription());
        this.setTelephone(entity.getTelephone());
        this.setEmail(entity.getEmail());

        return this;
    }

    public UserDetail with(UserEntity entity) {
        this.setId(entity.getId());
        this.setName(entity.getName());
        if (entity.getCreateTime() != null) {
            setCreateTime(entity.getCreateTime());
        }
        this.setUsername(entity.getUsername());
        this.setStatus(entity.getStatus());
        this.setType(UserEntityTypes.getType(entity.getType()));
        return this;
    }

    public UserDetail with(Authentication authentication) {
        this.setId(authentication.getUser().getId());
        this.setCreateTime(System.currentTimeMillis());
        this.setName(authentication.getUser().getName());
        this.setUsername(authentication.getUser().getUsername());
        return this;
    }

    public UserDetail withDimension(List<Dimension> details) {
        roleList = details
            .stream()
            .filter(dim -> DefaultDimensionType.role.isSameType(dim.getType()))
            .map(RoleInfo::of)
            .collect(Collectors.toList());

        orgList = details
            .stream()
            .filter(dim -> OrgDimensionType.org.isSameType(dim.getType()))
            .filter(dim -> dim
                .getOption("direct")
                .map(CastUtils::castBoolean)
                .orElse(false))
            .map(OrganizationInfo::of)
            .collect(Collectors.toList());

        return this;
    }

    public UserDetail withType() {
        this.setType(UserEntityTypes.getType(this.getTypeId()));
        return this;
    }

    public UserEntity toUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(id);
        userEntity.setName(name);
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        // 默认设置类型为普通用户
        if (type == null && !username.equals("admin")) {
            userEntity.setType(DefaultUserEntityType.USER.getId());
        }
        return userEntity;
    }

    public UserDetailEntity toDetailEntity() {
        return FastBeanCopier.copy(this, new UserDetailEntity());
    }

    @JsonIgnore
    public List<String> getOrgIdList() {
        return orgList == null ? null : orgList
            .stream()
            .map(OrganizationInfo::getId)
            .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<String> getRoleIdList() {
        return roleList == null ? null : roleList
            .stream()
            .map(RoleInfo::getId)
            .collect(Collectors.toList());
    }

}
