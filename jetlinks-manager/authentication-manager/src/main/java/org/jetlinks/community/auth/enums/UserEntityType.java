package org.jetlinks.community.auth.enums;

import org.springframework.util.StringUtils;

/**
 * 用户类型定义.
 *
 * @author zhangji 2022/12/7
 */
public interface UserEntityType {

    String getId();

    String getName();

    static UserEntityType of(String id,
                             String name) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        return DefaultUserEntityType
            .of(id)
            .map(type -> (UserEntityType) type)
            .orElseGet(() -> new UndefinedUserEntityType(id, StringUtils.hasText(name) ? name : id));
    }

}
