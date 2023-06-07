package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 默认用户类型.
 *
 * @author zhangji 2022/12/7
 */
@Getter
@AllArgsConstructor
public enum DefaultUserEntityType implements UserEntityType, EnumDict<String> {

    ADMIN("admin", "超级管理员"),
    USER("user", "普通用户"),
    OTHER("other", "其他");

    private final String id;

    private final String name;

    @Override
    public String getValue() {
        return id;
    }

    @Override
    public String getText() {
        return name;
    }

    static Optional<DefaultUserEntityType> of(String id) {
        return Arrays
            .stream(values())
            .filter(type -> type.getId().equals(id))
            .findAny();
    }

    @Override
    public Object getWriteJSONObject() {
        if (isWriteJSONObjectEnabled()) {
            Map<String, Object> jsonObject = new HashMap<>();
            jsonObject.put("id", getId());
            jsonObject.put("name", getName());
            return jsonObject;
        }
        return name();
    }
}
