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
package org.jetlinks.community.auth.enums;

import java.util.*;

/**
 * 用户类型管理.
 *
 * @author zhangji 2022/12/8
 */
public class UserEntityTypes {

    private static final Map<String, UserEntityType> types = new LinkedHashMap<>();

    public static void register(Collection<UserEntityType> type) {
        type.forEach(UserEntityTypes::register);
    }

    public static void register(UserEntityType type) {
        types.put(type.getId(), type);
    }

    public static UserEntityType getType(String id) {
        return types.getOrDefault(id, DefaultUserEntityType.OTHER);
    }

    public static List<UserEntityType> getAllType() {
        return new ArrayList<>(types.values());
    }

    public static UserEntityType of(String id, String name) {
        return types.put(id, UserEntityType.of(id, name));
    }
}
