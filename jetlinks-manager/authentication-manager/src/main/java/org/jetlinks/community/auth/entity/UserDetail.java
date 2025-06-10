/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.jetlinks.community.auth.enums.*;
import org.jetlinks.community.auth.service.info.UserLoginInfo;
import org.jetlinks.reactor.ql.utils.CastUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息详情
 *
 * @author zhouhao
 * @since 1.0
 */
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
    private String typeId;

    @Schema(hidden = true)
    private UserEntityType type;

    @Schema(description = "用户状态。1启用，0禁用")
    private Byte status;

    @Schema(description = "是否授权")
    private Boolean loggedIn;

    @Schema(description = "最后一次请求时间")
    private Long lastRequestTime;

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
    private Long createTime;

    @Schema(description = "角色信息")
    private List<RoleInfo> roleList;

    @Schema(description = "所在机构(部门)信息")
    private List<OrganizationInfo> orgList;

    @Schema(description = "创建者ID")
    private String creatorId;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "修改人ID")
    private String modifierId;

    @Schema(description = "修改人名称")
    private String modifierName;

    @Schema(description = "修改时间")
    private Long modifyTime;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "生日")
    private Long birthday;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "居民身份证号")
    private String idNumber;

    @Schema(description = "公司")
    private String company;

    @Schema(description = "注册方式")
    private RegisterEnum register;

    @Schema(description = "登录信息")
    private UserLoginInfo loginInfo;

    @Schema(description = "关联时间")
    private Long relationTime;


    public static UserDetail of() {
        return EntityFactoryHolder.newInstance(UserDetail.class, UserDetail::new);
    }

    public static UserDetail of(UserEntity entity) {
        return of().with(entity);
    }

    public UserDetail with(UserDetailEntity entity) {
        FastBeanCopier.copy(entity, this);
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
        //角色
        roleList = details
            .stream()
            .filter(dim -> DefaultDimensionType.role.isSameType(dim.getType()))
            .map(RoleInfo::of)
            .collect(Collectors.toList());

        //组织
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

    public UserDetail withLastRequestTime(Long lastRequestTime) {
        if (lastRequestTime == null || lastRequestTime == 0) {
            this.setLoggedIn(false);
        } else {
            this.setLoggedIn(true);
            this.setLastRequestTime(lastRequestTime);
        }
        return this;
    }

    public UserDetail withUserLoginInfo(UserLoginInfo info) {
        this.setLoginInfo(info);
        return this;
    }

    public UserEntity toUserEntity() {
        UserEntity userEntity = EntityFactoryHolder.newInstance(UserEntity.class, UserEntity::new);
        if (StringUtils.isNotBlank(id)) {
            userEntity.setId(id);
        }
        userEntity.setName(name);
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userEntity.setStatus(status);
        if (type == null) {
            if (!username.equals("admin")) {
                // 默认设置类型为普通用户
                userEntity.setType(DefaultUserEntityType.USER.getId());
            }
        } else {
            userEntity.setType(type.getId());
        }
        return userEntity;
    }

    public UserDetailEntity toDetailEntity() {
        return FastBeanCopier.copy(this, new UserDetailEntity());
    }

    @JsonIgnore
    public List<String> getOrgIdList() {
        return orgList == null ? null : Lists.transform(orgList, OrganizationInfo::getId);
    }

    @JsonIgnore
    public List<String> getRoleIdList() {
        return roleList == null ? null : Lists.transform(roleList, RoleInfo::getId);
    }
}
