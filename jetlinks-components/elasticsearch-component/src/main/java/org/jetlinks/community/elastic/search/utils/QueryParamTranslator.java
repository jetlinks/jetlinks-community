package org.jetlinks.community.elastic.search.utils;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.parser.DefaultLinkTypeParser;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author zhouhao
 * @since 1.0
 **/
@Slf4j
public class QueryParamTranslator {

    static DefaultLinkTypeParser linkTypeParser = new DefaultLinkTypeParser();

    static Consumer<Term> doNotingParamConverter = (term -> {
    });

    static Map<String, BiConsumer<DataType, Term>> converter = new ConcurrentHashMap<>();

    static BiConsumer<DataType, Term> defaultDataTypeConverter = (type, term) -> {

    };

    public static QueryBuilder createQueryBuilder(QueryParam queryParam, ElasticSearchIndexMetadata metadata) {
        BoolQueryBuilder queryBuilders = QueryBuilders.boolQuery();
        Consumer<Term> paramConverter = doNotingParamConverter;
        if (metadata != null) {
            paramConverter = t -> {
                if (StringUtils.isEmpty(t.getColumn())) {
                    return;
                }
                PropertyMetadata property = metadata.getProperty(t.getColumn());
                if (null != property) {
                    DataType type = property.getValueType();
                    converter.getOrDefault(type.getId(), defaultDataTypeConverter).accept(type, t);
                }
            };
        }
        for (Term term : queryParam.getTerms()) {
            linkTypeParser.process(term, paramConverter, queryBuilders);
        }
        return queryBuilders;
    }
    public static SearchSourceBuilder convertSearchSourceBuilder(QueryParam queryParam, ElasticSearchIndexMetadata metadata) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        if (queryParam.isPaging()) {
            sourceBuilder.from(queryParam.getPageIndex() * queryParam.getPageSize());
            sourceBuilder.size(queryParam.getPageSize());
        }
        for (Sort sort : queryParam.getSorts()) {
            if (!StringUtils.isEmpty(sort.getName())) {
                sourceBuilder.sort(sort.getName(), SortOrder.fromString(sort.getOrder()));
            }
        }

        return sourceBuilder.query(createQueryBuilder(queryParam,metadata));
    }

}
