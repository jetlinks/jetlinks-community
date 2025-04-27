package org.jetlinks.community.timescaledb.metadata;

import io.r2dbc.postgresql.codec.Json;
import org.hswebframework.ezorm.rdb.codec.JsonValueCodec;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.jetlinks.community.utils.ObjectMappers;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class JsonbValueCodec extends JsonValueCodec {

    public JsonbValueCodec(boolean array) {
        super(array ? ArrayList.class : LinkedHashMap.class,
              array ? ObjectMappers.JSON_MAPPER
                  .getTypeFactory()
                  .constructCollectionType(ArrayList.class, Object.class) :
                  ObjectMappers.JSON_MAPPER
                      .getTypeFactory()
                      .constructMapType(LinkedHashMap.class, Object.class, Object.class));
    }

    @Override
    public Object encode(Object value) {
        Object obj = super.encode(value);
        if (obj == null) {
            return encodeNull();
        }
        return NativeSql.of("?::jsonb", obj);
    }

    @Override
    public Object encodeNull() {
        return NativeSql.of("null::jsonb");
    }

    @Override
    public Object decode(Object data) {
        if (data instanceof io.r2dbc.postgresql.codec.Json) {
            byte[] arr = ((Json) data).asArray();

            if (arr[0] == '[') {
                return ObjectMappers.parseJsonArray(arr, Object.class);
            }
            return ObjectMappers.parseJson(arr, Object.class);

        }
        return super.decode(data);
    }
}