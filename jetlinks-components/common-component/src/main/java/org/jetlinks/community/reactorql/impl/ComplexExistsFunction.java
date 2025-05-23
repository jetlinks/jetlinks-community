/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.reactorql.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequests;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.dml.FunctionColumn;
import org.hswebframework.ezorm.rdb.operator.dml.FunctionTerm;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.reactorql.aggregation.AggregationSupport;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.community.utils.ReactorUtils;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.reactor.ql.DefaultReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLMetadata;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.feature.FeatureId;
import org.jetlinks.reactor.ql.feature.ValueMapFeature;
import org.jetlinks.reactor.ql.supports.DefaultReactorQLMetadata;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
public class ComplexExistsFunction implements ValueMapFeature {

    //集合中的元素
    public static final String COL_ELEMENT = "_element";
    //集合自身
    public static final String COL_SELF = "_self";
    //原始行
    public static final String COL_ROW = "_row";

    public static final ComplexExistsFunction INSTANCE = new ComplexExistsFunction();

    public static final String function = "complex_exists";

    static {
        DefaultReactorQLMetadata.addGlobal(INSTANCE);
    }

    public static void register() {

    }

    public static ExistsSpec createExistsSpec(Object val) {
        if (val instanceof ExistsSpec) {
            return ((ExistsSpec) val);
        }
        if (val instanceof Map) {
            ExistsSpec spec = new ExistsSpec();
            spec.fromJson(new JSONObject((Map) val));
            return spec;
        }
        if (val instanceof String) {
            ExistsSpec spec = new ExistsSpec();
            spec.setFilter(TermExpressionParser.parse(val.toString()));
            return spec;
        }
        return FastBeanCopier.copy(val, new ExistsSpec());
    }

    private static ExistsProcessor createExprProcessor(Expression expr) {
        String sql;
        if (expr instanceof Select) {
            sql = expr.toString();
        } else if (expr instanceof SubSelect) {
            sql = ((SubSelect) expr).getSelectBody().toString();
        } else if (expr instanceof BinaryExpression) {
            sql = "select 1 from t where " + expr;
        } else {
            throw new UnsupportedOperationException("不支持的表达式:" + expr);
        }
        SqlRequest request = SqlRequests.of(sql);
        ReactorQL ql = ReactorQL
            .builder()
            .sql(request.getSql())
            .build();
        return new ReactorQLProcessor(request, ql);
    }

    private static ExistsProcessor createExprProcessor(String str) {
        if (str.startsWith("select") || str.startsWith("SELECT")) {
            SqlRequest request = SqlRequests.of(str);
            ReactorQL ql = ReactorQL
                .builder()
                .sql(request.getSql())
                .build();
            return new ReactorQLProcessor(request, ql);
        }
        ExistsSpec spec = new ExistsSpec();
        spec.setFilter(TermExpressionParser.parse(str));
        return spec.compile();
    }

    @Override
    public Function<ReactorQLRecord, Publisher<?>> createMapper(Expression expression, ReactorQLMetadata metadata) {

        net.sf.jsqlparser.expression.Function func = ((net.sf.jsqlparser.expression.Function) expression);
        ExpressionList args = func.getParameters();
        if (args == null || args.getExpressions() == null || args.getExpressions().size() < 2) {
            throw new UnsupportedOperationException("complex_exists函数参数错误");
        }
        Function<ReactorQLRecord, Mono<ExistsProcessor>> processorMapper;

        List<Function<ReactorQLRecord, Publisher<?>>> mappers = args
            .getExpressions()
            .stream()
            .skip(1)
            .map(expr -> ValueMapFeature.createMapperNow(expr, metadata))
            .collect(Collectors.toList());

        Expression firstExpr = args.getExpressions().get(0);
        //以字符串来定义 complex_exists('name is test')
        if (firstExpr instanceof StringValue) {
            Mono<ExistsProcessor> processor = Mono.just(createExprProcessor(((StringValue) firstExpr).getValue()));
            processorMapper = record -> processor;
        } else if (firstExpr instanceof NumericBind) {
            int argIndex = ((NumericBind) firstExpr).getBindId() - 1;
            processorMapper = record -> Mono.justOrEmpty(
                record.getContext().getParameter(argIndex).map(this::transformProcessor)
            );
        } else if (firstExpr instanceof JdbcParameter) {
            int argIndex = ((JdbcParameter) firstExpr).getIndex() - 1;
            processorMapper = record -> Mono.justOrEmpty(
                record.getContext().getParameter(argIndex).map(this::transformProcessor)
            );
        }
        // complex_exists(select 1 from dual where ,arr)
        else if (firstExpr instanceof JdbcNamedParameter) {
            String name = ((JdbcNamedParameter) firstExpr).getName();
            processorMapper = record -> Mono.justOrEmpty(
                transformProcessor(record.getContext().getParameter(name))
            );
        } else {
            Mono<ExistsProcessor> processor = Mono.just(createExprProcessor(firstExpr));
            processorMapper = record -> processor;
        }

        return record ->
            processorMapper
                .apply(record)
                .flatMap(processor -> {
                    if (mappers.size() == 1) {
                        return processor.apply(
                            record,
                            mappers.get(0).apply(record));
                    }
                    return processor.apply(
                        record,
                        Flux.fromIterable(mappers)
                            .flatMap(mapper -> mapper.apply(record))
                    );
                });
    }

    @Override
    public String getId() {
        return FeatureId.ValueMap.of(function).getId();
    }

    private ExistsProcessor transformProcessor(Object value) {
        if (value instanceof ExistsProcessor) {
            return (ExistsProcessor) value;
        }
        if (value instanceof ExistsSpec) {
            return ((ExistsSpec) value).compile();
        }
        return null;
    }

    public interface ExistsProcessor extends BiFunction<ReactorQLRecord, Publisher<?>, Mono<Boolean>> {
        @Override
        Mono<Boolean> apply(ReactorQLRecord record, Publisher<?> publisher);

        Mono<Boolean> apply(ReactorQLRecord record, List<?> list);
    }

    @AllArgsConstructor
    static class ReactorQLProcessor implements ExistsProcessor {
        private final SqlRequest request;
        private final ReactorQL ql;

        public Mono<Boolean> apply(ReactorQLRecord record, List<?> list) {

            Flux<?> data = Flux
                .fromIterable(list)
                .map(v -> {
                    Map<String, Object> _data = Maps.newHashMapWithExpectedSize(3);
                    _data.put(COL_ROW,record.getRecord());
                    _data.put(COL_ELEMENT, v);
                    _data.put(COL_SELF, list);

                    return _data;
                });

            DefaultReactorQLContext ctx = new DefaultReactorQLContext((t) -> data);
            for (Object parameter : request.getParameters()) {
                ctx.bind(parameter);
            }

            return ql.start(ctx)
                     .hasElements();
        }

        @Override
        public Mono<Boolean> apply(ReactorQLRecord record, Publisher<?> publisher) {
            if (publisher instanceof Mono) {
                return ((Mono<?>) publisher)
                    .map(ConverterUtils::convertToList)
                    .flatMap(list -> apply(record, list));
            }
            return Flux
                .from(publisher)
                .as(CastUtils::flatStream)
                .collectList()
                .flatMap(list -> apply(record, list));

        }

        @Override
        public String toString() {
            return request.toNativeSql();
        }
    }

    private static FunctionTerm convertFunctionTerm(Object obj) {
        if (obj instanceof FunctionTerm) {
            return (FunctionTerm) obj;
        }
        if (obj instanceof Map) {
            return convertFunctionTerm(new JSONObject((Map) obj));
        }
        throw new UnsupportedOperationException("不支持的类型:" + obj);
    }

    private static FunctionTerm convertFunctionTerm(JSONObject obj) {
        FunctionTerm term = new FunctionTerm();
        FastBeanCopier.copy(obj, term, "terms");
        JSONArray terms = obj.getJSONArray("terms");
        if (terms != null) {
            terms
                .forEach(o -> {
                    term.addTerm(convertFunctionTerm((JSONObject) o));
                });
        }
        return term;
    }

    @Getter
    @Setter
    public static class ExistsSpec implements Jsonable {
        //过滤
        private List<Term> filter;

        //聚合
        private List<FunctionTerm> aggregation;

        @Override
        public void fromJson(JSONObject json) {
            FastBeanCopier.copy(json, this, "aggregation");
            JSONArray aggregation = json.getJSONArray("aggregation");
            if (aggregation != null) {
                this.aggregation = aggregation
                    .stream()
                    .map(ComplexExistsFunction::convertFunctionTerm)
                    .collect(Collectors.toList());
            }
        }

        //todo 其他简便配置的方式?如: 任意满足,全部满足等

        public void walkTerms(Consumer<Term> consumer) {
            if (filter != null) {
                filter.forEach(consumer);
            }
            if (aggregation != null) {
                aggregation.forEach(consumer);
            }
        }

        private void applyAggregation(BatchSqlFragments fragments,
                                      AtomicInteger count,
                                      FunctionTerm agg,
                                      Map<String, String> distinct,
                                      Map<Term, String> aliasMapping) {
            // 大小写都支持
            AggregationSupport support = AggregationSupport.getNow(agg.getFunction());

            FunctionColumn col = new FunctionColumn();
            col.setFunction(agg.getFunction());
            col.setColumn("this['" + agg.getColumn() + "']");
            col.setOpts(agg.getOpts() == null ? null : Maps.transformValues(agg.getOpts(), Object.class::cast));

            SqlFragments frg = support.createSql(col);
            String sqlStr = frg.toRequest().toNativeSql();
            String alias = distinct.get(frg.toRequest().toNativeSql());

            if (alias == null) {
                alias = "_agg_" + count.incrementAndGet();
                distinct.put(sqlStr, alias);
                aliasMapping.put(agg, alias);
                if (count.get() > 1) {
                    fragments.addSql(",");
                }
                fragments.add(frg).addSql(alias);
            }

            if (CollectionUtils.isNotEmpty(agg.getTerms())) {
                for (Term term : agg.getTerms()) {
                    if (term instanceof FunctionTerm) {
                        applyAggregation(fragments,
                                         count,
                                         ((FunctionTerm) term),
                                         distinct,
                                         aliasMapping);
                    }
                }
            }

        }

        private List<Term> createHavingTerm(List<? extends Term> aggregation,
                                            Map<Term, String> aliasMapping) {
            List<Term> terms = new ArrayList<>(aggregation.size());
            for (Term functionTerm : aggregation) {
                Term term = new Term();
                term.setColumn(aliasMapping.getOrDefault(functionTerm, functionTerm.getColumn()));
                term.setTermType(functionTerm.getTermType());
                term.setValue(functionTerm.getValue());
                term.setOptions(functionTerm.getOptions());
                term.setType(Term.Type.and);
                if (CollectionUtils.isNotEmpty(functionTerm.getTerms())) {
                    term.setTerms(createHavingTerm(functionTerm.getTerms(), aliasMapping));
                }
                terms.add(term);
            }
            return terms;
        }

        public ExistsProcessor compile() {

            SqlFragments cols;
            SqlFragments having;

            if (CollectionUtils.isNotEmpty(aggregation)) {
                Map<String, String> distinct = new HashMap<>();
                Map<Term, String> aliasMapping = new HashMap<>();

                BatchSqlFragments cols_ = new BatchSqlFragments();
                AtomicInteger count = new AtomicInteger(1);
                count.incrementAndGet();
                cols_.addSql(COL_SELF,",",COL_ELEMENT);
                for (FunctionTerm functionTerm : aggregation) {
                    applyAggregation(cols_, count, functionTerm, distinct, aliasMapping);
                }
                cols = cols_;
                List<Term> havingTerms = createHavingTerm(aggregation, aliasMapping);
                having = ReactorUtils.createFilterSql(havingTerms);
            } else {
                cols = SqlFragments.ONE;
                having = EmptySqlFragments.INSTANCE;
            }

            SqlFragments where = ReactorUtils.createFilterSql(filter);

            BatchSqlFragments fragments = new BatchSqlFragments();

            if (having.isNotEmpty()) {
                fragments.addSql("select 1 from (");
            }

            fragments.addSql("select").addFragments(cols).addSql("from t");
            if (where.isNotEmpty()) {
                fragments.addSql("where").addFragments(where);
            }

            if (having.isNotEmpty()) {
                fragments.add(SqlFragments.RIGHT_BRACKET)
                         .add(SqlFragments.WHERE)
                         .addFragments(having);
            }
            SqlRequest request = fragments.toRequest();

            ReactorQL ql = ReactorQL.builder()
                                    .sql(request.getSql())
                                    .build();

            return new ReactorQLProcessor(request, ql);
        }
    }


}
