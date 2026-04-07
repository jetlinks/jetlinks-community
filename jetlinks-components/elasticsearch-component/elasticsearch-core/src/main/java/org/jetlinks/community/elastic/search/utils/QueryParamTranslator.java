/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.elastic.search.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.util.ObjectBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermType;
import org.jetlinks.community.elastic.search.enums.ElasticSearchTermTypes;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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

        queryBuilder.bool(bool -> {
            if (CollectionUtils.isEmpty(terms)) {
                return bool;
            }
            TermGroup group = groupTerms(terms);
            return group.build(group.getType(), fParamConverter, fNestedConverter, bool);
        });

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

    private static Query.Builder applyTerm(Term term, Query.Builder builder, BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> converter) {

        if (term.getColumn().contains(".")) {
            String path = term.getColumn().split("[.]")[0];

            builder.nested(n -> converter.apply(term, n
                .path(path)
                .boost(1F)
                .ignoreUnmapped(false)
                .query(query -> ElasticSearchTermTypes
                    .lookup(term)
                    .map(type -> type.process(term, query))
                    .orElse(query))));

            return builder;
        }
        return ElasticSearchTermTypes.lookup(term).map(type -> type.process(term, builder)).orElse(builder);
    }

    public static TermGroup groupTerms(List<Term> terms) {
        return groupTerms(terms.get(0), terms.subList(1, terms.size()), new TermGroup(Term.Type.and));
    }

    private static TermGroup groupTerms(Term first, List<Term> others, TermGroup currentGroup) {
        if (first.getValue() != null) {
            currentGroup.addTerm(first);
        }
        if (!first.getTerms().isEmpty()) {
            currentGroup.addGroup(groupTerms(first.getTerms()));
        }

        if (CollectionUtils.isEmpty(others)) {
            return currentGroup;
        }

        Term current = others.get(0);
        //接下来为or,则新建or分组
        if (current.getType() == Term.Type.or) {
            TermGroup oldGroup = currentGroup;
            currentGroup = new TermGroup(Term.Type.or);
            //添加之前的分组
            currentGroup.addGroup(oldGroup);
            //添加后续条件
            currentGroup.addGroup(groupTerms(others));
            return currentGroup;
        } else {
            //在当前分组继续添加后续条件
            return groupTerms(current, others.subList(1, others.size()), currentGroup);
        }
    }

    @Getter
    @Setter
    public static class TermGroup {
        public TermGroup(Term.Type type) {
            this.type = type;
            this.terms = new ArrayList<>();
            this.groups = new ArrayList<>();
        }

        //只需处理第一层value,下级Term已拆分为group
        List<Term> terms;

        Term.Type type;

        List<TermGroup> groups;

        public void addTerm(Term term) {
            terms.add(term);
        }

        public void addGroup(TermGroup group) {
            if (group.getType() == type) {
                //同类型平铺
                for (Term term : group.getTerms()) {
                    addTerm(term);
                }
                groups.addAll(group.getGroups());
            } else if (group.getTerms().size() == 1 && group.getGroups().isEmpty()) {
                //不同类型但只有一个条件，直接添加
                addTerm(group.getTerms().get(0));
            } else {
                groups.add(group);
            }
        }

        public BoolQuery.Builder buildByType(Term.Type type, BoolQuery.Builder queryBuilders, Function<Query.Builder, ObjectBuilder<Query>> fn) {
            if (type == Term.Type.and) {
                return queryBuilders.must(fn);
            } else {
                return queryBuilders.should(fn);
            }
        }

        public BoolQuery.Builder build(Term.Type type,
                                       Consumer<Term> consumer,
                                       BiFunction<Term, NestedQuery.Builder, NestedQuery.Builder> nestedConverter,
                                       BoolQuery.Builder queryBuilders) {
            return buildByType(type, queryBuilders, builder -> builder.bool(_b -> {
                if (!terms.isEmpty()) {
                    for (Term term : terms) {
                        consumer.accept(term);
                        buildByType(this.getType(), _b, __b -> {
                            consumer.accept(term);
                            return applyTerm(term, __b, nestedConverter);
                        });
                    }

                }
                if (!groups.isEmpty()) {
                    for (TermGroup group : groups) {
                        group.build(this.getType(), consumer, nestedConverter, _b);
                    }
                }

                return _b;
            }));
        }
    }

    public static List<Long> resolveQueryTimestampRange(String property, List<Term> terms) {

        long from = 0;
        long to = 0;
        for (Term term : terms) {

            if (Objects.equals(term.getColumn(), property)) {
                if (Objects.equals(term.getTermType(), TermType.gt) ||
                    Objects.equals(term.getTermType(), TermType.gte)) {
                    from = CastUtils.castDate(term.getValue()).getTime();
                } else if (Objects.equals(term.getTermType(), TermType.lt) ||
                    Objects.equals(term.getTermType(), TermType.lte)) {
                    to = CastUtils.castDate(term.getValue()).getTime();
                } else if (Objects.equals(term.getTermType(), TermType.btw)) {
                    List<Long> range = ConverterUtils.convertToList(
                        term.getValue(),
                        val -> CastUtils.castDate(val).getTime());
                    if (range.size() >= 2) {
                        return range;
                    }
                }
            }
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(term.getTerms())) {
                List<Long> range = resolveQueryTimestampRange(property, term.getTerms());
                if (range != null && range.size() >= 2) {
                    return range;
                }
            }
        }

        if (from == 0) {
            return null;
        }
        if (to == 0) {
            to = System.currentTimeMillis();
        }
        return Arrays.asList(from, to);
    }

}