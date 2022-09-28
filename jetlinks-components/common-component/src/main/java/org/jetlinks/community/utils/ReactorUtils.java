package org.jetlinks.community.utils;

import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 响应式相关工具类
 *
 * @author zhouhao
 * @since 2.0
 */
public class ReactorUtils {


    public static <T> Function<Flux<T>, Flux<T>> limit(Long pageIndex, Long pageSize) {
        if (pageIndex == null || pageSize == null) {
            return Function.identity();
        }
        return flux -> flux.skip(pageIndex & pageSize).take(pageSize);
    }

    /**
     * 构造有效期内去重的Flux
     *
     * <pre>
     *    flux.as(ReactorUtils.distinct(MyData::getId,Duration.ofSeconds(30)))
     * </pre>
     *
     * @param keySelector 去重的key
     * @param duration    有效期
     * @param <T>         泛型
     * @return 去重构造器
     */
    public static <T> Function<Flux<T>, Flux<T>> distinct(Function<T, ?> keySelector, Duration duration) {
        return FluxUtils.distinct(keySelector, duration);
    }


    public static final Function<Object, Mono<Boolean>> alwaysTrue = ignore -> Reactors.ALWAYS_TRUE;


    /**
     * 使用 {@link Term}来构造一个异步过滤器,请缓存过滤器函数使用,不要每次构建.
     * <p>
     * 在判断时会尝试把对象转为Map,
     *
     * <pre>{@code
     *
     * flux
     *  .filter(createFilter(TermExpressionParser.parse("age gt 1 and name like '%张%'")))
     *  .flatMap(this::handleData)
     *  ...
     *
     * }</pre>
     *
     * @param terms 条件对象
     * @param <T>   对象泛型
     * @return 过滤器函数
     */
    @SuppressWarnings("all")
    public static <T> Function<T, Mono<Boolean>> createFilter(List<Term> filter) {
        return createFilter(filter, t -> {
            if (t instanceof Map) {
                return ((Map<String, Object>) t);
            }
            if (t instanceof Jsonable) {
                return ((Jsonable) t).toJson();
            }
            return FastBeanCopier.copy(t, new HashMap<>());
        });
    }

    /**
     * 使用 {@link Term}来构造一个异步过滤器,请缓存过滤器函数使用,不要每次构建
     *
     * <pre>{@code
     *
     * flux
     *  .filter(createFilter(TermExpressionParser.parse("age gt 1 and name like '%张%'"),Data::toMap))
     *  .flatMap(this::handleData)
     *  ...
     *
     * }</pre>
     *
     * @param terms     条件对象
     * @param converter 转换器，用于将对象转为map,更有利于进行条件判断
     * @param <T>       对象泛型
     * @return 过滤器函数
     */
    @SuppressWarnings("all")
    public static <T> Function<T, Mono<Boolean>> createFilter(List<Term> terms,
                                                              Function<T, Map<String, Object>> converter) {

        return createFilter(terms, converter, (arg, data) -> arg);
    }

    @SuppressWarnings("all")
    public static <T> Function<T, Mono<Boolean>> createFilter(List<Term> terms,
                                                              Function<T, Map<String, Object>> converter,
                                                              BiFunction<Object, Map<String, Object>, Object> bindConverter) {
        if (CollectionUtils.isEmpty(terms)) {
            return (Function<T, Mono<Boolean>>) alwaysTrue;
        }

        SqlFragments fragments = termBuilder.createTermFragments(null, terms);
        if (fragments.isEmpty()) {
            return (Function<T, Mono<Boolean>>) alwaysTrue;
        }

        SqlRequest request = fragments.toRequest();

        String sql = "select 1 from dual where " + request.getSql();
        String nativeSql = request.toNativeSql();
        try {
            ReactorQL ql = ReactorQL.builder().sql(sql).build();
            Object[] parameters = request.getParameters();
            return new Function<T, Mono<Boolean>>() {
                @Override
                public String toString() {
                    return nativeSql;
                }

                @Override
                public Mono<Boolean> apply(T data) {
                    Map<String, Object> mapValue = converter.apply(data);
                    ReactorQLContext context = ReactorQLContext.ofDatasource(ignore -> Flux.just(mapValue));
                    for (Object parameter : parameters) {
                        context.bind(bindConverter.apply(parameter, mapValue));
                    }
                    return ql
                        .start(context)
                        .hasElements();
                }
            };
        } catch (Throwable e) {
            throw new IllegalArgumentException("error.create_connector_filter_error", e);
        }
    }

    static final TermBuilder termBuilder = new TermBuilder();

    static class TermBuilder extends AbstractTermsFragmentBuilder<Object> {

        @Override
        public SqlFragments createTermFragments(Object parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(Object trigger, Term term) {
            String termType = StringUtils.hasText(term.getTermType()) ? term.getTermType() : "eq";
            switch (termType) {
                case "is":
                case "=":
                    termType = "eq";
                    break;
                case ">":
                    termType = "gt";
                    break;
                case ">=":
                    termType = "gte";
                    break;
                case "<":
                    termType = "lt";
                    break;
                case "<=":
                    termType = "lte";
                    break;
                case "!=":
                case "<>":
                    termType = "neq";
                    break;
            }
            try {
                TermTypeSupport support = TermTypeSupport.valueOf(termType);
                return support.createSql("this['" + term.getColumn() + "']", term.getValue());
            } catch (Throwable e) {
                throw new IllegalArgumentException("unsupported termType " + term.getTermType(), e);
            }
        }
    }

    @Getter
    enum TermTypeSupport {

        eq("等于", "eq"),
        neq("不等于", "neq"),

        gt("大于", "gt", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID),
        gte("大于等于", "gte", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID),
        lt("小于", "lt", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID),
        lte("小于等于", "lte", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID),

        btw("在...之间", "btw", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID) {
            @Override
            protected Object convertValue(Object val) {
                return val;
            }
        },
        nbtw("不在...之间", "nbtw", DateTimeType.ID, IntType.ID, FloatType.ID, DoubleType.ID) {
            @Override
            protected Object convertValue(Object val) {
                return val;
            }
        },
        in("在...之中", "in", StringType.ID, IntType.ID, FloatType.ID, DoubleType.ID) {
            @Override
            protected Object convertValue(Object val) {
                return val;
            }
        },
        nin("不在...之中", "not in", StringType.ID, IntType.ID, FloatType.ID, DoubleType.ID) {
            @Override
            protected Object convertValue(Object val) {
                return val;
            }
        },

        like("包含字符", "str_like", StringType.ID),
        nlike("不包含字符", "not str_like", StringType.ID),

        ;

        private final String text;
        private final Set<String> supportTypes;
        private final String function;

        TermTypeSupport(String text, String function, String... supportTypes) {
            this.text = text;
            this.function = function;
            this.supportTypes = new HashSet<>(Arrays.asList(supportTypes));
        }

        protected Object convertValue(Object val) {

            return val;
        }

        public final SqlFragments createSql(String column, Object value) {
            PrepareSqlFragments fragments = PrepareSqlFragments.of();
            fragments.addSql(function + "(", column, ",");
            if (value instanceof NativeSql) {
                fragments
                    .addSql(((NativeSql) value).getSql())
                    .addParameter(((NativeSql) value).getParameters());
            } else {
                fragments.addSql("?")
                         .addParameter(convertValue(value));
            }
            fragments.addSql(")");
            return fragments;
        }

    }

}
