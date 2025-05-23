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
package org.jetlinks.community.dictionary;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.defaults.DefaultItemDefine;

import java.io.IOException;
import java.util.Map;

public class DictionaryJsonDeserializer extends JsonDeserializer<EnumDict<?>> {
    @Override
    public EnumDict<?> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        if (jsonParser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            DefaultItemDefine defaultItemDefine = new DefaultItemDefine();
            defaultItemDefine.setOrdinal(jsonParser.getIntValue());
            return defaultItemDefine;
        }
        if (jsonParser.hasToken(JsonToken.VALUE_STRING)) {
            String str = jsonParser.getText().trim();
            if (!str.isEmpty()) {
                DefaultItemDefine defaultItemDefine = new DefaultItemDefine();
                defaultItemDefine.setValue(str);
                return defaultItemDefine;
            }
        }

        if (jsonParser.hasToken(JsonToken.START_OBJECT)) {
            Map<?, ?> map = ctxt.readValue(jsonParser, Map.class);
            if (map != null) {
                return FastBeanCopier.copy(map, new DefaultItemDefine());
            }
        }
        return null;
    }
}
