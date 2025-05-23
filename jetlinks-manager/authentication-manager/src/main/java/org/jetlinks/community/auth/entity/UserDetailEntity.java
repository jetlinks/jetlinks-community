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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.things.ThingInfo;
import org.jetlinks.community.auth.enums.GenderEnum;
import org.jetlinks.community.auth.enums.RegisterEnum;
import org.jetlinks.community.relation.RelationConstants;

import javax.persistence.Column;
import javax.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Table(name = "s_user_detail")
@Comment("用户详情信息表")
@Getter
@Setter
@EnableEntityEvent
public class UserDetailEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Schema(description = "姓名")
    @Column(nullable = false)
    @NotBlank(message = "姓名不能为空", groups = {CreateGroup.class})
    private String name;

    @Schema(description = "邮箱")
    @Column
    @Email(message = "邮件格式错误")
    private String email;

    @Schema(description = "电话")
    @Column(length = 32)
    private String telephone;

    @Schema(description = "头像图片地址")
    @Column(length = 2000)
    @URL(message = "头像格式错误")
    private String avatar;

    @Schema(description = "说明")
    @Column(length = 2000)
    private String description;

    @Schema(description = "创建人ID")
    @Column(length = 64, updatable = false)
    private String creatorId;

    @Schema(description = "创建人名称")
    @Column
    @Upsert(insertOnly = true)
    private String creatorName;

    @Schema(description = "创建时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Column(updatable = false)
    private Long createTime;

    @Column(length = 64)
    @Schema(description = "修改人ID")
    private String modifierId;

    @Column
    @Schema(description = "修改人名称")
    private String modifierName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "修改时间")
    private Long modifyTime;

    @Column(length = 32)
    @ColumnType(javaType = String.class)
    @EnumCodec
    @DefaultValue("unknown")
    @Schema(description = "性别")
    private GenderEnum gender;

    @Column
    @Schema(description = "生日")
    private Long birthday;

    @Column
    @Schema(description = "真实姓名")
    private String realName;

    @Column
    @Schema(description = "居民身份证号")
    @Pattern(regexp = "(^[1-9]\\d{7}(0[1-9]|1[0-2])([0-2][1-9]|[1-2]0|31)\\d{3}$)|(^[1-9]\\d{5}[1-9]\\d{3}(0[1-9]|1[0-2])([0-2][1-9]|[1-2]0|31)(\\d{4}|\\d{3}[Xx])$)",
        message = "请输入正确的身份证号", groups = CreateGroup.class)
    private String idNumber;

    @Column(length = 32)
    @ColumnType(javaType = String.class)
    @EnumCodec
    @DefaultValue("backstage")
    @Schema(description = "注册方式")
    private RegisterEnum register;

    @Column
    @Schema(description = "公司")
    private String company;


    public static UserDetailEntity of() {
        return EntityFactoryHolder.newInstance(UserDetailEntity.class, UserDetailEntity::new);
    }

    public ThingInfo toThingInfo() {
        return ThingInfo
            .builder()
            .id(getId())
            .name(getName())
            .build()
            .addConfig(RelationConstants.UserProperty.email, getEmail())
            .addConfig(RelationConstants.UserProperty.telephone, getTelephone())
            .addConfig("avatar", getAvatar())
            ;
    }
}
