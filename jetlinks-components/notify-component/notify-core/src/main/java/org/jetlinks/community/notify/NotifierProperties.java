package org.jetlinks.community.notify;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * 通知配置属性
 *
 * @author zhouhao
 * @see NotifyConfigManager
 * @since 1.0
 */
@Getter
@Setter
public class NotifierProperties implements ValueObject, Serializable {

    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 配置全局唯一标识
     */
    private String id;

    /**
     * 通知类型标识
     *
     * @see NotifyType
     */
    private String type;

    /**
     * 通知服务提供商标识,如: aliyun ...
     */
    private String provider;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置内容,不同的服务提供商,配置不同.
     *
     * @see NotifierProvider
     */
    private Map<String, Object> configuration;

    @Deprecated
    public Optional<Object> getConfig(String key) {
        return Optional.ofNullable(configuration)
            .map(conf -> conf.get(key));
    }

    @Deprecated
    public Object getConfigOrNull(String key) {
        return Optional.ofNullable(configuration)
            .map(conf -> conf.get(key))
            .orElse(null);
    }

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
