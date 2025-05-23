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
package org.jetlinks.community.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.jetlinks.core.metadata.DataType;

import java.io.IOException;
import java.util.Map;

/**
 *
 * @author zhangji 2025/1/23
 * @since 2.3
 */
public class DataTypeJSONDeserializer extends JsonDeserializer<DataType> {
    @Override
    public DataType deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        Map<String,Object> map= ctxt.readValue(parser, Map.class);

        return (DataType) BeanUtilsBean.getInstance().getConvertUtils().convert(map, DataType.class);
    }
}
