package org.jetlinks.community.things.data;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultMetricMetadataManager implements MetricMetadataManager {
    private final Map<String, Map<String, PropertyMetadata>> repo = new ConcurrentHashMap<>();

    @Override
    public void register(String metric, List<PropertyMetadata> properties) {
        repo.compute(metric, (key, old) -> {
            if (old != null) {
                old.clear();
            } else {
                old = new ConcurrentHashMap<>();
            }
            for (PropertyMetadata property : properties) {
                old.put(property.getId(), property);
            }
            return old;
        });
    }

    @Override
    public Optional<PropertyMetadata> getColumn(String metric, String property) {
        if (metric == null || property == null) {
            return Optional.empty();
        }
        Map<String, PropertyMetadata> m = repo.get(metric);
        if (m != null) {
            return Optional.ofNullable(m.get(property));
        }
        return Optional.empty();
    }
}
