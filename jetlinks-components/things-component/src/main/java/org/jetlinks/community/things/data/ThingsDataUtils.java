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
package org.jetlinks.community.things.data;

import org.jetlinks.community.timeseries.utils.TimeSeriesUtils;
import org.jetlinks.community.Interval;
import org.jetlinks.community.timeseries.utils.TimeSeriesUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;
import java.util.function.BiFunction;

public class ThingsDataUtils {

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

    public static NavigableMap<Long, Map<String, Object>> prepareAggregationData(AggregationRequest request,
                                                                                 PropertyAggregation... properties) {

        return prepareAggregationData(request, TimeSeriesUtils::truncateTime, properties);
    }

    public static NavigableMap<Long, Map<String, Object>> prepareAggregationData(AggregationRequest request,
                                                                                 BiFunction<Long, Interval, Long> timeTruncate,
                                                                                 PropertyAggregation... properties) {
        NavigableMap<Long, Map<String, Object>> data = new TreeMap<>(Comparator.comparingLong(l -> -l));
        Map<String, Object> valueMap = new HashMap<>();
        for (PropertyAggregation property : properties) {
            valueMap.put(property.getAlias(), property.getDefaultValue());
        }
        if (request.getInterval() == null) {
            data.put(0L,valueMap);
            return data;
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(request.getFormat());

        long startWith = request.getFrom().getTime();
        long endWith = request.getTo().getTime();

        Iterable<Long> iterable = request.getInterval().iterate(startWith, endWith);
        for (Long time : iterable) {
            time = timeTruncate.apply(time, request.getInterval());
            Map<String, Object> tempData = new HashMap<>(valueMap);
            //使用joad-time，有的格式java.time不支持...
            String formatValue = new DateTime(new Instant(time), DateTimeZone.getDefault()).toString(formatter);
            tempData.put("time", formatValue);
            data.put(time, tempData);
        }

        return data;
    }

}
