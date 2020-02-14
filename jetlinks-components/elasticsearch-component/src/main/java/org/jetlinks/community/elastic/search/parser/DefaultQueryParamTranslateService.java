package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import org.jetlinks.community.elastic.search.index.mapping.SingleMappingMetadata;
import org.jetlinks.community.elastic.search.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DefaultQueryParamTranslateService extends AbstractQueryParamTranslateService {


    private final LinkTypeParser parser;

    @Value("${jetlinks.system.formats:yyyy-MM-dd HH:mm:ss}")
    private List<String> formats;

    @Autowired
    public DefaultQueryParamTranslateService(LinkTypeParser parser) {
        this.parser = parser;
    }

    @Override
    protected QueryBuilder queryBuilder(QueryParam queryParam, IndexMappingMetadata mappingMetaData) {
        BoolQueryBuilder queryBuilders = QueryBuilders.boolQuery();
        queryParam.getTerms()
                .forEach(term -> parser.process(term, t ->
                        dateTypeHandle(t, mappingMetaData.getMetaData(t.getColumn())), queryBuilders));
        return queryBuilders;
    }


    private void dateTypeHandle(Term term, SingleMappingMetadata singleMappingMetaData) {
        if (singleMappingMetaData == null) return;
        if (singleMappingMetaData.getType().equals(FieldType.DATE)) {
            term.setValue(DateTimeUtils.formatDateToTimestamp(term.getValue(), formats));
        }
    }
}
