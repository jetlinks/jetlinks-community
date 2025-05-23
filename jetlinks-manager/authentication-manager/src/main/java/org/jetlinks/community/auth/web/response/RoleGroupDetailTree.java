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
package org.jetlinks.community.auth.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.entity.RoleGroupEntity;

import java.util.List;
import java.util.Locale;

/**
 * @author gyl
 * @since 2.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class RoleGroupDetailTree {

    private String groupId;
    private String groupName;
    private List<RoleEntity> roles;


    public static RoleGroupDetailTree of(RoleGroupEntity group) {
        RoleGroupDetailTree roleGroupDetailTree = new RoleGroupDetailTree();
        roleGroupDetailTree.setGroupId(group.getId());
        roleGroupDetailTree.setGroupName(group.getName());
        return roleGroupDetailTree;
    }

    public static RoleGroupDetailTree of(RoleGroupEntity group, Locale locale) {
        RoleGroupDetailTree roleGroupDetailTree = new RoleGroupDetailTree();
        roleGroupDetailTree.setGroupId(group.getId());
        roleGroupDetailTree.setGroupName(group.getI18nName(locale));
        return roleGroupDetailTree;
    }


}
