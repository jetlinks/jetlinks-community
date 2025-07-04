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
package org.jetlinks.community.things.data;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.PropertyMetadata;

public interface ThingsDataConstants {

    String COLUMN_ID = "id";
    String COLUMN_THING_ID = "thingId";
    String COLUMN_TEMPLATE_ID = "templateId";

    String COLUMN_PROPERTY_ID = "property";
    String COLUMN_PROPERTY_TYPE = "type";
    String COLUMN_PROPERTY_VALUE = "value";
    String COLUMN_PROPERTY_NUMBER_VALUE = "numberValue";
    String COLUMN_PROPERTY_TIME_VALUE = "timeValue";
    String COLUMN_PROPERTY_GEO_VALUE = "geoValue";
    String COLUMN_PROPERTY_ARRAY_VALUE = "arrayValue";
    String COLUMN_PROPERTY_OBJECT_VALUE = "objectValue";


    String COLUMN_EVENT_ID = "event";
    String COLUMN_EVENT_VALUE = "value";


    String COLUMN_TIMESTAMP = "timestamp";
    String COLUMN_CREATE_TIME = "createTime";
    String COLUMN_MESSAGE_ID = "messageId";
    String COLUMN_LOG_TYPE = "type";
    String COLUMN_LOG_CONTENT = "content";


    ConfigKey<String> storePolicyConfigKey = ConfigKey.of("storePolicy");

    String propertyStorageType = "storageType";
    String propertyStorageTypeJson = "json-string";
    String propertyStorageTypeIgnore = "ignore";

    /**
     * 判断属性是否使用json字符串来存储
     *
     * @param metadata 属性物模型
     * @return 是否使用json字符串存储
     */
    static boolean propertyIsJsonStringStorage(PropertyMetadata metadata) {
        return metadata
            .getExpand(propertyStorageType)
            .map(propertyStorageTypeJson::equals)
            .orElse(false);
    }

    /**
     * 判断属性是否忽略存储
     *
     * @param metadata 属性物模型
     * @return 属性是否忽略存储
     */
    static boolean propertyIsIgnoreStorage(PropertyMetadata metadata) {
        return metadata
            .getExpand(propertyStorageType)
            .map(propertyStorageTypeIgnore::equals)
            .orElse(false);
    }
}
