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
package org.jetlinks.community.auth.configuration;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.jetlinks.community.auth.enums.UserEntityType;

import java.io.IOException;
import java.util.Map;

/**
 * 用户类型-反序列化.
 *
 * @author zhangji 2022/12/8
 */
public class UserEntityTypeJSONDeerializer extends JsonDeserializer<UserEntityType> {
    @Override
    public UserEntityType deserialize(JsonParser jsonParser,
                                      DeserializationContext ctxt) throws IOException, JacksonException {
        if (jsonParser.hasToken(JsonToken.VALUE_STRING)) {
            String str = jsonParser.getText().trim();
            if (str.length() != 0) {
                return UserEntityType.of(str, null);
            }
        }

        if (jsonParser.hasToken(JsonToken.START_OBJECT)) {
            Map<String, String> map = ctxt.readValue(jsonParser, Map.class);
            if (map != null) {
                return UserEntityType.of(map.get("id"), map.get("name"));
            }
        }
        return null;
    }
}
