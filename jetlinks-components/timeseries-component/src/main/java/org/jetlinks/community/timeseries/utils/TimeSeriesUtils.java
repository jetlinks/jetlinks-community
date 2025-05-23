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
package org.jetlinks.community.timeseries.utils;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.Interval;
import org.jetlinks.community.timeseries.query.AggregationColumn;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.community.timeseries.query.TimeGroup;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;

public class TimeSeriesUtils {


    public static Map<String, Object> findAggregationData(long ts,
                                                          NavigableMap<Long, Map<String, Object>> container) {

        Map<String, Object> prepare = container.get(ts);
        if (prepare != null) {
            return prepare;
        }
        Map.Entry<Long, Map<String, Object>> entry = container.floorEntry(ts);
        if (entry != null) {
            return entry.getValue();
        }
        return null;
    }

    public static long truncateTime(long time, Interval interval) {
        return interval.getUnit().truncatedTo(time);
    }

    public static NavigableMap<Long, Map<String, Object>> prepareAggregationData(AggregationQueryParam request,
                                                                                 TimeGroup group) {

        return prepareAggregationData(request, group, TimeSeriesUtils::truncateTime);
    }

    public static NavigableMap<Long, Map<String, Object>> prepareAggregationData(AggregationQueryParam request,
                                                                                 TimeGroup group,
                                                                                 BiFunction<Long, Interval, Long> timeTruncate) {
        NavigableMap<Long, Map<String, Object>> data = new TreeMap<>(Comparator.comparingLong(l -> -l));
        Map<String, Object> valueMap = new HashMap<>();
        for (AggregationColumn property : request.getAggColumns()) {
            valueMap.put(property.getAlias(), property.getAggregation().getDefaultValue());
        }
        if (group == null || group.getInterval() == null) {
            data.put(0L, valueMap);
            return data;
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(group.getFormat());

        long startWith = request.getStartWithTime();
        long endWith = request.getEndWithTime();

        Iterable<Long> iterable = group.getInterval().iterate(startWith, endWith);
        for (Long time : iterable) {
            time = timeTruncate.apply(time, group.getInterval());
            Map<String, Object> tempData = new HashMap<>(valueMap);
            String formatValue = new DateTime(new Instant(time), DateTimeZone.getDefault()).toString(formatter);
            tempData.put(group.getAlias(), formatValue);
            data.put(time, tempData);
        }

        return data;
    }

    public static void prepareAggregationQueryParam(AggregationQueryParam param) {
        if (param.getStartWithTime() == 0) {
            param.setStartWithTime(calculateStartWithTime(param));
        }
    }

    public static long calculateStartWithTime(AggregationQueryParam param) {
        long startWithParam = param.getStartWithTime();
        if (startWithParam == 0) {
            //从查询条件中提取时间参数来获取时间区间
            List<Term> terms = param.getQueryParam().getTerms();
            for (Term term : terms) {
                if ("timestamp".equals(term.getColumn())) {
                    Object value = term.getValue();
                    String termType = term.getTermType();
                    if (TermType.btw.equals(termType)) {
                        if (String.valueOf(value).contains(",")) {
                            value = Arrays.asList(String.valueOf(value).split(","));
                        }
                        return DateTimeType.GLOBAL.convert(CastUtils.castArray(value).get(0)).getTime();
                    }
                    if (TermType.gt.equals(termType) || TermType.gte.equals(termType)) {

                        return DateTimeType.GLOBAL.convert(value).getTime();
                    }
                }
            }
            return param.getEndWithTime() - Duration.ofDays(90).toMillis();
        }
        return startWithParam;
    }

}
