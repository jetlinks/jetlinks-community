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
public class UserEntityTypeJSONDeserializer extends JsonDeserializer<UserEntityType> {
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
