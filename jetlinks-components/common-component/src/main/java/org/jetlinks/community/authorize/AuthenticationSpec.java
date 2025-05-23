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
package org.jetlinks.community.authorize;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Getter
@Setter
public class AuthenticationSpec implements Serializable {

    private static final long serialVersionUID = 3512105446265694264L;

    private RoleSpec role;

    private List<PermissionSpec> permissions;

    @Getter
    @Setter
    public static class RoleSpec {
        private List<String> idList;
    }

    @Getter
    @Setter
    public static class PermissionSpec implements Serializable {
        private static final long serialVersionUID = 7188197046015343251L;
        private String id;
        private List<String> actions;
    }

    public boolean isGranted(Authentication auth) {
        return createFilter().test(auth);
    }

    public Predicate<Authentication> createFilter() {
        RoleSpec role = this.role;
        List<PermissionSpec> permissions = this.permissions;
        List<Predicate<Authentication>> all = new ArrayList<>();

        if (null != role && role.getIdList() != null) {
            all.add(auth -> auth.hasDimension(DefaultDimensionType.role.getId(), role.getIdList()));
        }

        if (null != permissions) {
            for (PermissionSpec permission : permissions) {
                all.add(auth -> auth.hasPermission(permission.getId(), permission.getActions()));
            }
        }

        Predicate<Authentication> temp = null;
        for (Predicate<Authentication> predicate : all) {
            if (temp == null) {
                temp = predicate;
            } else {
                temp = temp.and(predicate);
            }
        }
        return temp == null ? auth -> true : temp;
    }
}
