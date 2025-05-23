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
package org.jetlinks.community.device.events.handler;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class ValueTypeTranslator {

    public static Object translator(Object value, DataType dataType) {
        try {
           if (dataType instanceof Converter<?>) {
                return ((Converter<?>) dataType).convert(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            log.error("设备上报值与物模型不匹配.value:{},type:{}", value, JSON.toJSONString(dataType), e);
            return value;
        }
    }

}
