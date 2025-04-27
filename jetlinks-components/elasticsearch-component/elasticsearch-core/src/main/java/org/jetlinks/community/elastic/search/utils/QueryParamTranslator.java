package org.jetlinks.community.elastic.search.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermType;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermTypes;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author zhouhao
 * @author bestfeng
 * @since 1.0
 **/
@Slf4j
public class QueryParamTranslator {

    static Consumer<Term> doNotingParamConverter = (term -> {
    });

    public static Query.Builder applyQueryBuilder(Query.Builder queryBuilder,
                                                  QueryParam queryParam,
                                                  ElasticSearchIndexMetadata metadata) {
        return applyQueryBuilder(queryBuilder, queryParam.getTerms(), metadata);
    }

    public static Query.Builder applyQueryBuilder(Query.Builder queryBuilder,
                                                  List<Term> terms,
                                                  ElasticSearchIndexMetadata metadata) {
        Consumer<Term> paramConverter = doNotingParamConverter;
        if (metadata != null) {
            paramConverter = term -> {
                if (ObjectUtils.isEmpty(term.getColumn())) {
                    return;
                }
                ElasticSearchTermType termType = ElasticSearchTermTypes.lookupNow(term);
                PropertyMetadata property = metadata.getProperty(term.getColumn());
                if (null != property) {
                    DataType type = property.getValueType();
                    ThingsDatabaseUtils.tryConvertTermValue(
                        type,
                        term,
                        termType::convertTermValue);
                }
            };
        }


        BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> nestedConverter = (a, b) -> b;
        if (metadata != null) {
            nestedConverter = (term, builder) -> {
                if (!ObjectUtils.isEmpty(term.getColumn())) {
                    return nestedTermConverter(term, metadata, builder);
                }
                return builder;
            };
        }
        Consumer<Term> fParamConverter = paramConverter;
        BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> fNestedConverter = nestedConverter;

        queryBuilder.bool(bool -> process(
            terms,
            fParamConverter,
            fNestedConverter, bool));

        return queryBuilder;
    }

    public static NestedQuery.Builder nestedTermConverter(Term term,
                                                          ElasticSearchIndexMetadata metadata,
                                                          NestedQuery.Builder queryBuilder) {
        ElasticSearchTermType termType = ElasticSearchTermTypes.lookup(term).orElse(null);
        if (termType != null) {
            PropertyMetadata property = metadata.getProperty(term.getColumn());
            if (null != property && (TermType.isnull.equals(termType.getId()) || TermType.notnull.equals(termType.getId()))) {
                if (property.getValueType() instanceof ArrayType arrayType) {
                    // nested类型无法直接使用exists判断
                    if (arrayType.getElementType() instanceof ObjectType) {
                        queryBuilder.scoreMode(ChildScoreMode.None);
                    }
                }
                if (property.getValueType() instanceof ObjectType) {
                    // nested类型无法直接使用exists判断
                    queryBuilder.scoreMode(ChildScoreMode.None);
                }
            }
        }
        return queryBuilder;

    }


    public static MultisearchBody.Builder convertSearchRequestBuilder(
        MultisearchBody.Builder builder,
        QueryParam queryParam,
        ElasticSearchIndexMetadata metadata) {

        if (queryParam.isPaging()) {
            builder.from(queryParam.getPageIndex() * queryParam.getPageSize());
            builder.size(queryParam.getPageSize());
        }
        for (Sort sort : queryParam.getSorts()) {
            builder.sort(_sort -> _sort.field(field -> field
                .field(sort.getName())
                .order(sort.getOrder()
                           .equalsIgnoreCase("ASC") ?
                           co.elastic.clients.elasticsearch._types.SortOrder.Asc : co.elastic.clients.elasticsearch._types.SortOrder.Desc)));
        }

        builder.query(query -> applyQueryBuilder(query, queryParam, metadata));

        return builder;
    }

    public static SearchRequest.Builder convertSearchRequestBuilder(
        SearchRequest.Builder builder,
        QueryParam queryParam,
        ElasticSearchIndexMetadata metadata) {

        if (queryParam.isPaging()) {
            builder.from(queryParam.getPageIndex() * queryParam.getPageSize());
            builder.size(queryParam.getPageSize());
        }
        for (Sort sort : queryParam.getSorts()) {
            builder.sort(_sort -> _sort.field(field -> field
                .field(sort.getName())
                .order(sort.getOrder()
                           .equalsIgnoreCase("ASC") ?
                           co.elastic.clients.elasticsearch._types.SortOrder.Asc : co.elastic.clients.elasticsearch._types.SortOrder.Desc)));
        }

        builder.query(query -> applyQueryBuilder(query, queryParam, metadata));

        return builder;
    }


    public static BoolQuery.Builder process(List<Term> terms,
                                            Consumer<Term> consumer,
                                            BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> nestedConverter,
                                            BoolQuery.Builder queryBuilders) {

        if (CollectionUtils.isEmpty(terms)) {
            return queryBuilders;
        }

        for (TermGroup group : groupTerms(terms)) {
            if (group.type == Term.Type.or) {
                for (Term groupTerm : group.getTerms()) {
                    handleOr(queryBuilders, groupTerm, nestedConverter, consumer);
                }
            } else {
                queryBuilders
                    .should(must ->
                                must.bool(bool -> {
                                    for (Term groupTerm : group.getTerms()) {
                                        handleAnd(bool, groupTerm, nestedConverter, consumer);
                                    }
                                    return bool;
                                }));
            }

        }
        return queryBuilders;
    }

    private static void handleOr(BoolQuery.Builder queryBuilders,
                                 Term term,
                                 BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> nestedConverter,
                                 Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {

            queryBuilders
                .should(should -> applyTerm(term, should, nestedConverter));

        } else if (!term.getTerms().isEmpty()) {

            queryBuilders
                .should(should -> should
                    .bool(bool -> process(term.getTerms(), consumer, nestedConverter, bool)));
        }
    }

    private static Query.Builder applyTerm(Term term, Query.Builder builder, BiFunction<Term, NestedQuery.Builder,
        NestedQuery.Builder> converter) {

        if (term.getColumn().contains(".")) {
            String path = term.getColumn().split("[.]")[0];

            builder.nested(n -> converter
                .apply(term, n
                    .path(path)
                    .boost(1F)
                    .ignoreUnmapped(false)
                    .query(query -> ElasticSearchTermTypes
                        .lookup(term)
                        .map(type -> type.process(term, query))
                        .orElse(query)))
            );

            return builder;
        }
        return ElasticSearchTermTypes
            .lookup(term)
            .map(type -> type.process(term, builder))
            .orElse(builder);
    }

    private static void handleAnd(BoolQuery.Builder queryBuilders,
                                  Term term,
                                  BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> nestedConverter,
                                  Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {

            queryBuilders.must(must -> applyTerm(term, must, nestedConverter));

        } else if (!term.getTerms().isEmpty()) {

            queryBuilders.must(must -> must.bool(bool -> process(term.getTerms(), consumer, nestedConverter, bool)));
        }
    }


    public static Set<TermGroup> groupTerms(List<Term> terms) {
        Set<TermGroup> groups = new HashSet<>();
        TermGroup currentGroup = null;

        for (int i = 0; i < terms.size(); i++) {
            Term currentTerm = terms.get(i);
            Term nextTerm = (i + 1 < terms.size()) ? terms.get(i + 1) : null;

            if (currentTerm.getType() == Term.Type.or) {
                if (currentGroup == null || currentGroup.type == Term.Type.and) {
                    currentGroup = new TermGroup(Term.Type.or);
                }
                // 如果下一个Term为"AND"，创建一个新分组
                if (nextTerm != null && nextTerm.getType() == Term.Type.and) {
                    currentGroup = new TermGroup(Term.Type.and);
                }
            } else {
                if (currentGroup == null) {
                    currentGroup = new TermGroup(Term.Type.and);
                }
            }
            currentGroup.addTerm(currentTerm);
            groups.add(currentGroup);
        }
        return groups;
    }

    @Getter
    @Setter
    public static class TermGroup {
        public TermGroup(Term.Type type) {
            this.type = type;
            this.terms = new ArrayList<>();
        }

        List<Term> terms;

        Term.Type type;

        public void addTerm(Term term) {
            terms.add(term);
        }
    }


}