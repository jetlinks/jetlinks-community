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
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.parser.DefaultLinkTypeParser;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.springframework.util.ObjectUtils;
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

    private static boolean maybeList(Term term) {
        switch (term.getTermType().toLowerCase()) {
            case TermType.in:
            case TermType.nin:
            case TermType.btw:
            case TermType.nbtw:
                return true;
        }
        return false;
    }

    private static boolean isDoNotConvertValue(Term term) {
        switch (term.getTermType().toLowerCase()) {
            case TermType.isnull:
            case TermType.notnull:
            case TermType.empty:
            case TermType.nempty:
                return true;
        }
        return false;
    }


    private static Object convertValue(DataType type, Object val) {
        if (type instanceof DateTimeType) {
            return TimeUtils.convertToDate(val).getTime();
        } else if (type instanceof Converter) {
            return ((Converter<?>) type).convert(val);
        }
        return val;
    }

    public static QueryBuilder createQueryBuilder(QueryParam queryParam, ElasticSearchIndexMetadata metadata) {
        BoolQueryBuilder queryBuilders = QueryBuilders.boolQuery();
        Consumer<Term> paramConverter = doNotingParamConverter;
        if (metadata != null) {
            paramConverter = t -> {
                if (ObjectUtils.isEmpty(t.getColumn()) || isDoNotConvertValue(t)) {
                    return;
                }
                PropertyMetadata property = metadata.getProperty(t.getColumn());
                if (null != property) {
                    DataType type = property.getValueType();

                    Object value;
                    if (maybeList(t)) {
                        value = ConverterUtils.tryConvertToList(t.getValue(), v -> convertValue(type, v));
                    } else {
                        value = convertValue(type, t.getValue());
                    }
                    if (null != value) {
                        t.setValue(value);
                    } else {
                        log.warn("Can not convert {} to {}", t.getValue(), type.getId());
                    }
                    converter.getOrDefault(type.getId(), defaultDataTypeConverter).accept(type, t);
                }
            };
        }
        linkTypeParser.process(queryParam.getTerms(), paramConverter, queryBuilders);
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

        return sourceBuilder.query(createQueryBuilder(queryParam, metadata));
    }

}
