package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import org.springframework.util.StringUtils;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public abstract class AbstractQueryParamTranslateService implements QueryParamTranslateService {

    @Override
    public SearchSourceBuilder translate(QueryParam queryParam, IndexMappingMetadata mappingMetaData) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        if (queryParam.isPaging()) {
            sourceBuilder.from(queryParam.getPageIndex() * queryParam.getPageSize());
            sourceBuilder.size(queryParam.getPageSize());
        }
        queryParam.getSorts()
                .forEach(sort -> {
                    if (!StringUtils.isEmpty(sort.getName())) {
                        sourceBuilder.sort(sort.getName(), SortOrder.fromString(sort.getOrder()));
                    }

                });
        return sourceBuilder.query(queryBuilder(queryParam, mappingMetaData));
    }

    protected abstract QueryBuilder queryBuilder(QueryParam queryParam, IndexMappingMetadata mappingMetaData);

}
