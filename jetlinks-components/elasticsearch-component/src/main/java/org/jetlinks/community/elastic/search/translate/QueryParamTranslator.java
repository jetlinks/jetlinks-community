package org.jetlinks.community.elastic.search.translate;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.enums.LinkTypeEnum;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class QueryParamTranslator {

    public static QueryBuilder translate(QueryParam queryParam) {

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        Objects.requireNonNull(queryParam, "QueryParam must not null.")
                .getTerms()
                .forEach(term -> LinkTypeEnum.of(term.getType().name())
                        .ifPresent(e -> e.process(query, term)));
        return query;
    }

    public static SearchSourceBuilder transSourceBuilder(QueryParam queryParam) {
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
        return sourceBuilder.query(translate(queryParam));
    }
}
