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
package org.jetlinks.community;

import lombok.Generated;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.message.HeaderKey;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author wangzheng
 * @since 1.0
 */
@Generated
public interface PropertyConstants {
    //机构ID
    Key<String> orgId = Key.of("orgId");

    //设备名称
    Key<String> deviceName = Key.of("deviceName");

    //产品名称
    Key<String> productName = Key.of("productName");

    //产品ID
    Key<String> productId = Key.of("productId");

    /**
     * 关系信息.值格式:
     * <pre>{@code
     * [{"type":"user","id":"userId","rel":"manager"}]
     * }</pre>
     */
    Key<List<Map<String, Object>>> relations = Key.of("relations");

    //分组ID
    Key<List<String>> groupId = Key.of("groupId");

    //是否记录task记录
    Key<Boolean> useTask = Key.of("useTask", false);

    //taskId
    Key<String> taskId = Key.of("taskId");

    //最大重试次数
    Key<Long> maxRetryTimes = Key.of("maxRetryTimes", () -> Long.getLong("device.message.task.retryTimes", 1), Long.class);
    //当前重试次数
    Key<Long> retryTimes = Key.of("retryTimes", () -> 0L, Long.class);

    //服务ID
    Key<String> serverId = Key.of("serverId");

    //全局唯一ID
    Key<String> uid = Key.of("_uid", IDGenerator.RANDOM::generate);

    //设备接入网关ID
    Key<String> accessId = Key.of("accessId");

    /**
     * 设备接入方式
     *
     * @see org.jetlinks.community.gateway.supports.DeviceGatewayProvider#getId
     */
    Key<String> accessProvider = Key.of("accessProvider");

    //设备创建者
    Key<String> creatorId = Key.of("creatorId");

    @SuppressWarnings("all")
    static <T> Optional<T> getFromMap(ConfigKey<T> key, Map<String, Object> map) {
        return Optional.ofNullable((T) map.get(key.getKey()));
    }

    @SuppressWarnings("all")
    static <T> T getFromMapOrElse(ConfigKey<T> key, Map<String, Object> map, Supplier<T> defaultIfEmpty) {
        Object value = map.get(key.getKey());
        if (value == null) {
            return defaultIfEmpty.get();
        }
        return (T) value;
    }

    @Generated
    interface Key<V> extends ConfigKey<V>, HeaderKey<V> {

        @Override
        default Type getValueType() {
            return ConfigKey.super.getValueType();
        }

        @Override
        default Class<V> getType() {
            return ConfigKey.super.getType();
        }

        static <T> Key<T> of(String key) {
            return new Key<T>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public T getDefaultValue() {
                    return null;
                }
            };
        }

        static <T> Key<T> of(String key, T defaultValue) {
            return new Key<T>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public T getDefaultValue() {
                    return defaultValue;
                }
            };
        }

        static <T> Key<T> of(String key, Supplier<T> defaultValue) {
            return new Key<T>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public T getDefaultValue() {
                    return defaultValue == null ? null : defaultValue.get();
                }
            };
        }

        static <T> Key<T> of(String key, Supplier<T> defaultValue, Type type) {
            return new Key<T>() {

                @Override
                public Type getValueType() {
                    return type;
                }

                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public T getDefaultValue() {
                    return defaultValue == null ? null : defaultValue.get();
                }
            };
        }


    }
}
