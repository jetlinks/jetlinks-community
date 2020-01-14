package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface QueryParamTranslateService {

    SearchSourceBuilder translate(QueryParam queryParam, IndexMappingMetadata metaData);
}
