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

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Dimension;

@Getter
@Setter
public class RoleInfo {

    private String id;
    private String name;

    public static RoleInfo of() {
        return EntityFactoryHolder.newInstance(RoleInfo.class, RoleInfo::new);
    }

    public RoleInfo with(Dimension dimension) {
        RoleInfo info = this;
        info.setId(dimension.getId());
        info.setName(dimension.getName());
        return info;
    }

    public static RoleInfo of(Dimension dimension) {
        return of().with(dimension);
    }

    public static RoleInfo of(RoleEntity role) {
        return role.copyTo(of());
    }
}
