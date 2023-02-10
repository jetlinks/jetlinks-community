package org.jetlinks.community.network;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class NetworkProperties implements Serializable, ValueObject {
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private String id;

    /**
     * 网络类型
     * @see NetworkType#getId()
     */
    private String type;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置是否启用
     */
    private boolean enabled;

    /**
     * 配置内容，不同的网络组件，配置内容不同
     */
    private Map<String, Object> configurations;

    @Override
    public Map<String, Object> values() {
        return configurations;
    }
}
