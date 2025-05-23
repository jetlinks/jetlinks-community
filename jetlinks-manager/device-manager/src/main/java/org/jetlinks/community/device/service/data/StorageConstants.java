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
package org.jetlinks.community.device.service.data;

import org.jetlinks.core.metadata.PropertyMetadata;

public interface StorageConstants {
    String storePolicyConfigKey = "storePolicy";

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
