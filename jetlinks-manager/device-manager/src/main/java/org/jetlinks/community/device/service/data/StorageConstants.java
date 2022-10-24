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
