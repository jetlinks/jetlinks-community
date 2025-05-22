package org.jetlinks.community.plugin;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.util.Map;

@Getter
@Setter
public class PluginDriverConfig implements ValueObject {
    private String id;
    private String provider;
    private Map<String,Object> configuration;

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
