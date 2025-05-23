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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;

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
public enum DefaultUserEntityType implements UserEntityType, I18nEnumDict<String> {

    ADMIN("admin", "超级管理员"),
    USER("user", "普通用户"),
    APPLICATION("application", "第三方用户"),
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
            jsonObject.put("name", getI18nMessage(LocaleUtils.current()));
            return jsonObject;
        }
        return name();
    }
}
