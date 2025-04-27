package org.jetlinks.community.elastic.search;

import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.transport.Version;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermTypes;
import org.jetlinks.community.elastic.search.enums.ElasticSearch8xTermType;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;

public class ElasticSearch8xSupport extends ElasticSearchSupport {

    static {
        Version version = Version.VERSION;
        if (version != null && version.major() == 8) {
            for (ElasticSearch8xTermType value : ElasticSearch8xTermType.values()) {
                ElasticSearchTermTypes.register(value);
            }
        }

    }

    @Override
    public boolean is8x() {
        return true;
    }

    @Override
    public IndexSettings.Builder applyIndexSettings(ElasticSearchIndexProperties index, IndexSettings.Builder builder) {
        return super
            .applyIndexSettings(index, builder)
            .mapping(b -> b
                .totalFields(t -> t.limit(index.getTotalFieldsLimit())));
    }

    @Override
    public DynamicTemplate createDynamicTemplate(String type, Property property) {
        return DynamicTemplate
            .of(b -> b
                .matchMappingType(type)
                .mapping(property));
    }

    @Override
    public TemplateMapping getTemplateMapping(GetTemplateResponse response, String index) {
        return response.get(index);
    }

    @Override
    public IndexState getIndexState(GetIndexResponse response, String index) {
        return response.get(index);
    }

    @Override
    public IndexMappingRecord getIndexMapping(GetMappingResponse response, String index) {
        return response.get(index);
    }

    @Override
    protected Object getBucketKey(MultiBucketBase bucket) {
        if (bucket instanceof DateHistogramBucket _bucket) {
            return _bucket.key();
        }
        if (bucket instanceof HistogramBucket _bucket) {
            return _bucket.key();
        }
        return null;
    }
}
