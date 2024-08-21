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
