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
package org.jetlinks.community.auth.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.UpdateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.auth.entity.UserDetail;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * 创建用户请求，用于新建用户操作
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
public class SaveUserRequest {

    @Schema(description = "用户基本信息")
    @NotNull
    private UserDetail user;

    @Schema(description = "角色ID列表")
    private Set<String> roleIdList;

    @Schema(description = "机构ID列表")
    private Set<String> orgIdList;

    public SaveUserRequest validate() {
        if (user == null) {
            throw new IllegalArgumentException("user can not be null");
        }
        if (StringUtils.hasText(user.getId())) {
            ValidatorUtils.tryValidate(user, UpdateGroup.class);
        } else {
            ValidatorUtils.tryValidate(user, CreateGroup.class);
        }
        return this;
    }
}
