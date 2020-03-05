package org.jetlinks.community.elastic.search.index.mapping;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.elastic.search.enums.ElasticPropertyType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class IndexMappingMetadata {

    private String index;

    private Map<String, SingleMappingMetadata> metadata = new HashMap<>();

    public List<SingleMappingMetadata> getAllMetaData() {
        return metadata.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public SingleMappingMetadata getMetaData(String fieldName) {
        return metadata.get(fieldName);
    }

    public List<SingleMappingMetadata> getMetaDataByType(ElasticPropertyType type) {
        return getAllMetaData()
                .stream()
                .filter(singleMapping -> singleMapping.getType().equals(type))
                .collect(Collectors.toList());
    }

    public Map<String, SingleMappingMetadata> getMetaDataByTypeToMap(ElasticPropertyType type) {
        return getMetaDataByType(type)
                .stream()
                .collect(Collectors.toMap(SingleMappingMetadata::getName, Function.identity()));
    }

    public void setMetadata(SingleMappingMetadata singleMapping) {
        metadata.put(singleMapping.getName(), singleMapping);
    }


    private IndexMappingMetadata(String index) {
        this.index = index;
    }

    private IndexMappingMetadata() {
    }

    public static IndexMappingMetadata getInstance(String index) {
        return new IndexMappingMetadata(index);
    }
}
