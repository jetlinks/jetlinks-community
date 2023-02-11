package org.jetlinks.community.things.data;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;
import java.util.Optional;

public interface MetricMetadataManager {

    void register(String metric, List<PropertyMetadata> properties);

    Optional<PropertyMetadata> getColumn(String metric, String property);

}
