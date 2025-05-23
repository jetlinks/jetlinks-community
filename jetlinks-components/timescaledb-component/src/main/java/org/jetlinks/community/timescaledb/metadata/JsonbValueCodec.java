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