package org.jetlinks.community.elastic.search.index.mapping;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class IndicesMappingCenter {

    private Map<String, IndexMappingMetadata> indicesMapping = new ConcurrentHashMap<>();


    public Optional<IndexMappingMetadata> getIndexMappingMetaData(String index) {
        return Optional.ofNullable(indicesMapping.get(index));
    }

    public void register(IndexMappingMetadata mappingMetaData) {
        indicesMapping.put(mappingMetaData.getIndex(), mappingMetaData);
    }
}
