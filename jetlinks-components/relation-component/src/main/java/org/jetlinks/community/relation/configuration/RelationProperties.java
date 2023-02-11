package org.jetlinks.community.relation.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关系配置
 *
 * @author zhouhao
 * @since 2.0
 */
@ConfigurationProperties(prefix = "relation")
@Getter
@Setter
public class RelationProperties {

    /**
     * <pre>{@code
     * relation:
     *     relatable:
     *          device: user,org
     * }</pre>
     * 可建立关系的对象类型映射
     */
    private Map<String, List<String>> relatable = new HashMap<>();


    public List<String> getRelatable(String type) {
        return relatable.getOrDefault(type, Collections.emptyList());
    }
}
