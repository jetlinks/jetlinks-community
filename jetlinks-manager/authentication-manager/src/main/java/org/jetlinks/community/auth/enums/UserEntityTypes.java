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
