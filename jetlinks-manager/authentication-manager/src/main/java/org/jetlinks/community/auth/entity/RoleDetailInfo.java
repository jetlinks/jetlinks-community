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

/**
 * @author: tangchao
 * @since: 2.2
 */
@Getter
@Setter
public class RoleDetailInfo {

    @Schema(description = "角色id")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "所属分组")
    private String groupId;

    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "修改人ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String modifierId;

    @Schema(description = "修改时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long modifyTime;

    @Schema(description = "成员数")
    private Integer memberSelfCount = 0;

    public static RoleDetailInfo from(RoleEntity role) {
        return role.copyTo(new RoleDetailInfo());
    }
}
