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
