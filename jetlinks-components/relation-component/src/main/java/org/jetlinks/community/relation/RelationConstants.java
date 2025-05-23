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
package org.jetlinks.community.relation;

import org.jetlinks.core.config.ConfigKey;

import java.util.List;

public interface RelationConstants {

    interface UserProperty {
        ConfigKey<String> id = ConfigKey.of("id", "ID", String.class);
        ConfigKey<String> username = ConfigKey.of("username", "用户名", String.class);
        ConfigKey<String> name = ConfigKey.of("name", "名称", String.class);
        ConfigKey<String> email = ConfigKey.of("email", "邮箱", String.class);
        ConfigKey<String> telephone = ConfigKey.of("telephone", "手机号码", String.class);
        ConfigKey<Object> all = ConfigKey.of("*", "全部信息", Object.class);
        ConfigKey<List<String>> departments = ConfigKey.of("departments", "所在部门");
        ConfigKey<List<String>> roles = ConfigKey.of("roles", "角色");

        static ConfigKey<String> thirdParty(String type, String provider) {
            return ConfigKey.of(String.join(".", "third", type, provider), "第三方用户ID", String.class);
        }
    }

    interface DeviceProperty {
        static ConfigKey<String> lastProperty(String property) {
            return ConfigKey.of(String.join(".", "property", property), "最新属性", String.class);
        }
    }
}
