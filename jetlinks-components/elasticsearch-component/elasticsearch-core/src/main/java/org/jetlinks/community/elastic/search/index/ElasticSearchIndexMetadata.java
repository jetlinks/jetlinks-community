package org.jetlinks.community.elastic.search.index;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;

import java.util.List;
import java.util.Map;

public interface ElasticSearchIndexMetadata {

    String getIndex();

    List<PropertyMetadata> getProperties();

    PropertyMetadata getProperty(String property);

    default Map<String, Object> convertToElastic(Map<String, Object> map) {
        return ElasticSearchConverter.convertDataToElastic(map, getProperties());
    }

    default Map<String, Object> convertFromElastic(Map<String, Object> map) {
        return ElasticSearchConverter.convertDataFromElastic(map, getProperties());
    }

    default ElasticSearchIndexMetadata newIndexName(String name) {
        return new DefaultElasticSearchIndexMetadata(name, getProperties());
    }
}
