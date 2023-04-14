package org.jetlinks.community.tdengine.things;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.utils.time.DefaultDateFormatter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.tdengine.TDEngineUtils;
import org.jetlinks.community.tdengine.term.TDengineQueryConditionBuilder;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.tdengine.Point;
import org.jetlinks.community.tdengine.TDengineOperations;
import org.jetlinks.community.things.data.MetricMetadataManager;
import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Predicate3;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

@AllArgsConstructor
class TDengineThingDataHelper implements Disposable {

    static {
        DateFormatter.supportFormatter.add(new DefaultDateFormatter(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.+"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

    }

    final TDengineOperations operations;

    final MetricMetadataManager metadataManager;

    //转换聚合函数
    public static String convertAggFunction(PropertyAggregation agg) {
        switch (agg.getAgg()) {
            case NONE:
                throw new BusinessException("error.unsupported_aggregation_condition", 500, agg);
            default:
                return agg.getAgg().name().toLowerCase();
        }
    }

    public static boolean isArrayTerm(DataType type, Term term) {
        String termType = term.getTermType().toLowerCase();
        return TermType.btw.equals(termType)
            || TermType.nbtw.equals(termType)
            || TermType.in.equals(termType)
            || TermType.nin.equals(termType);
    }

    public static String getGroupByTime(Interval interval) {
        return "interval(" + interval.toString() + ")";
    }


    public static Object tryConvertList(DataType type, Term term) {
        return ConverterUtils
            .tryConvertToList(term.getValue(), val -> {
                if (type instanceof Converter) {
                    return ((Converter<?>) type).convert(val);
                }
                return val;
            });
    }

    //预处理查询条件
    public List<Term> prepareTerms(String metric, List<Term> terms) {

        if (CollectionUtils.isEmpty(terms)) {
            return terms;
        }
        for (Term term : terms) {
            //适配时间戳字段,查询统一使用timestamp
            if (("timestamp".equals(term.getColumn()) || "_ts".equals(term.getColumn())) && term.getValue() != null) {
                term.setColumn("_ts");
                term.setValue(prepareTimestampValue(term.getValue(), term.getTermType()));
            } else {
                metadataManager
                    .getColumn(metric, term.getColumn())
                    .ifPresent(meta -> {
                        DataType type = meta.getValueType();
                        if (isArrayTerm(type, term)) {
                            term.setValue(tryConvertList(type, term));
                        } else if (type instanceof Converter) {
                            term.setValue(((Converter<?>) type).convert(term.getValue()));
                        }
                    });
            }

            term.setTerms(prepareTerms(metric, term.getTerms()));
        }

        return terms;
    }

    public static Object prepareTimestampValue(Object value, String type) {

        return ConverterUtils.tryConvertToList(value, v -> {
            Date date = CastUtils.castDate(v);
            return TDEngineUtils.formatTime(date.getTime());
        });
    }

    public static String buildOrderBy(QueryParamEntity param) {

        for (Sort sort : param.getSorts()) {
            if (sort.getName().equalsIgnoreCase("timestamp")) {
                return " order by `_ts` " + sort.getOrder();
            }
        }
        return " order by `_ts` desc";
    }

    public String buildWhere(String metric, QueryParamEntity param, String... and) {
        StringJoiner joiner = new StringJoiner(" ", "where ", "");

        String sql = TDengineQueryConditionBuilder.build(prepareTerms(metric, param.getTerms()));

        if (StringUtils.hasText(sql)) {
            joiner.add(sql);
        }

        if (StringUtils.hasText(sql) && and.length > 0) {
            joiner.add("and");
        }
        for (int i = 0; i < and.length; i++) {
            if (i > 0) {
                joiner.add("and");
            }
            joiner.add(and[i]);
        }

        return joiner.length() == 6 ? "" : joiner.toString();
    }

    public Flux<TimeSeriesData> query(String sql) {
        return operations
            .forQuery()
            .query(sql, ResultWrappers.map())
            .mapNotNull(TDengineThingDataHelper::convertToTsData);
    }

    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {
        QueryParamEntity param = query.getParam();
        StringJoiner joiner = new StringJoiner("");
        joiner.add("select * from")
            .add(" `")
            .add(metric)
            .add("` ")
            .add(buildWhere(metric, param))
            .add(buildOrderBy(param));

        if (param.isPaging()) {
            joiner.add(" limit ").add(String.valueOf(param.getPageSize()))
                .add(" offset ")
                .add(String.valueOf(param.getPageSize() * param.getPageIndex()));
        }
        return operations
            .forQuery()
            .query(joiner.toString(), ResultWrappers.map())
            .mapNotNull(TDengineThingDataHelper::convertToTsData);
    }

    public static TimeSeriesData convertToTsData(Map<String, Object> map) {
        Date ts = convertTs(map.remove("_ts"));
        return TimeSeriesData.of(ts, map);
    }

    protected <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                   Query<?, QueryParamEntity> query,
                                                   Function<TimeSeriesData, T> mapper) {
        QueryParamEntity param = query.getParam();
        String sql = "`" + metric + "` " + buildWhere(metric, param);
        String countSql = "select count(1) total from " + sql;
        String dataSql = "select * from " + sql + buildOrderBy(param) + " limit " + param.getPageSize() + " offset " + param
            .getPageIndex() * param.getPageSize();

        return Mono.zip(
            operations.forQuery()
                .query(countSql, ResultWrappers.map())
                .singleOrEmpty()
                .map(data -> CastUtils.castNumber(data.getOrDefault("total", 0)).intValue())
                .defaultIfEmpty(0),
            operations.forQuery()
                .query(dataSql, ResultWrappers.map())
                .mapNotNull(map -> mapper.apply(convertToTsData(map)))
                .collectList(),
            (total, data) -> PagerResult.of(total, data, param)
        );
    }

    private static final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static Date convertTs(Object ts) {
        if (ts == null) {
            throw new IllegalArgumentException();
        }
        if (ts instanceof Number) {
            return new Date(((Number) ts).longValue());
        }

        return DateTime.parse(String.valueOf(ts)).toDate();
    }


    //转换值为influxDB field或者tag
    public void applyValue(Point builder,
                           String metric,
                           String key,
                           Object value,
                           Predicate3<String, String, Object> tagTest) {
        if (value == null) {
            return;
        }
        if (tagTest.test(metric, key, value)) {
            builder.tag(key, String.valueOf(value));
        } else if (value instanceof Number) {
            builder.value(key, ((Number) value).doubleValue());
        } else if (value instanceof Date) {
            builder.value(key, ((Date) value).getTime());
        } else {
            if (value instanceof String) {
                builder.value(key, String.valueOf(value));
            } else {
                builder.value(key, ObjectMappers.toJsonString(value));
            }
        }
    }

    public Point convertToPoint(String metric, TimeSeriesData data, Predicate3<String, String, Object> tagTest) {
        Point point = Point.of(metric, null)
            .timestamp(data.getTimestamp());

        for (Map.Entry<String, Object> entry : data.values().entrySet()) {
            applyValue(point, metric, entry.getKey(), entry.getValue(), tagTest);
        }
        return point;
    }

    public Mono<Void> doSave(String metric, TimeSeriesData data, Predicate3<String, String, Object> tagTest) {
        return operations.forWrite().write(convertToPoint(metric, data, tagTest));
    }


    public Mono<Void> doSave(String metric, Flux<TimeSeriesData> dataFlux, Predicate3<String, String, Object> tagTest) {

        return operations.forWrite().write(dataFlux.map(data -> convertToPoint(metric, data, tagTest)));
    }


    @Override
    public void dispose() {
        operations.dispose();
    }
}
