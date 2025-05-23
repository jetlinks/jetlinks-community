package org.jetlinks.community.plugin.context;

import org.jetlinks.plugin.core.PluginEnvironment;
import org.jetlinks.community.ValueObject;

import java.util.Map;
import java.util.Optional;

public class SimplePluginEnvironment implements PluginEnvironment {
    private final ValueObject properties;

    public SimplePluginEnvironment(Map<String, Object> properties) {
        this(ValueObject.of(properties));
    }

    public SimplePluginEnvironment(ValueObject properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> getProperty(String key) {
        return properties.getString(key);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) {

        return properties.get(key, type);
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties.values();
    }
}
