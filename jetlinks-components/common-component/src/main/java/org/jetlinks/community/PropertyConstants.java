package org.jetlinks.community;

import lombok.Generated;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.message.HeaderKey;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author wangzheng
 * @since 1.0
 */
public interface PropertyConstants {
    Key<String> orgId = Key.of("orgId");

    Key<String> deviceName = Key.of("deviceName");
    //产品名称
    Key<String> productName = Key.of("productName");

    Key<String> productId = Key.of("productId");
    Key<String> uid = Key.of("_uid");
    //设备创建者
    Key<String> creatorId = Key.of("creatorId");

    //设备接入网关ID
    Key<String> accessId = Key.of("accessId");

    /**
     * 设备接入方式
     * @see org.jetlinks.community.gateway.supports.DeviceGatewayProvider#getId
     */
    Key<String> accessProvider = Key.of("accessProvider");

    @SuppressWarnings("all")
    static <T> Optional<T> getFromMap(ConfigKey<T> key, Map<String, Object> map) {
        return Optional.ofNullable((T) map.get(key.getKey()));
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
                    return defaultValue.get();
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
                    return defaultValue.get();
                }
            };
        }

    }
}
