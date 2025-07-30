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
package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.*;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MeterTimeSeriesData implements TimeSeriesData {

    private final Map<String, Object> data = new HashMap<>(24);

    private final long timestamp = System.currentTimeMillis();

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    public MeterTimeSeriesData name(String name) {
        data.put("name", name);

        return this;
    }

    public MeterTimeSeriesData write(Map<String, String> tags) {
        if (tags != null) {
            data.putAll(tags);
        }
        return this;
    }

    public MeterTimeSeriesData write(List<Tag> tags, Function<String, DataType> typeGetter) {
        for (Tag tag : tags) {
            DataType type = typeGetter.apply(tag.getKey());
            Object value = tag.getValue();
            if (type instanceof Converter) {
                value = ((Converter<?>) type).convert(value);
            }
            data.put(tag.getKey(), value);
        }
        return this;
    }

    public MeterTimeSeriesData write(Counter counter) {
        double value = counter.count();
        if (Double.isFinite(value)) {
            data.put("count", value);
        }
        return this;
    }

    public MeterTimeSeriesData write(Gauge gauge) {
        double value = gauge.value();
        if (Double.isFinite(value)) {
            data.put("value", value);
        }
        return this;
    }

    public MeterTimeSeriesData write(TimeGauge gauge) {
        double value = gauge.value(TimeUnit.MILLISECONDS);
        if (Double.isFinite(value)) {
            data.put("value", value);
        }
        return this;
    }

    public MeterTimeSeriesData write(FunctionTimer timer) {
        data.put("count", timer.count());
        data.put("sum", timer.totalTime(TimeUnit.MILLISECONDS));
        data.put("mean", timer.mean(TimeUnit.MILLISECONDS));

        return this;
    }


    public MeterTimeSeriesData write(LongTaskTimer timer) {

        data.put("activeTasks", timer.activeTasks());
        data.put("duration", timer.duration(TimeUnit.MILLISECONDS));

        return this;
    }

    public MeterTimeSeriesData write(Timer timer) {
        data.put("count", timer.count());
        data.put("sum", timer.totalTime(TimeUnit.MILLISECONDS));
        data.put("mean", timer.mean(TimeUnit.MILLISECONDS));
        data.put("max", timer.max(TimeUnit.MILLISECONDS));
        return this;
    }

    public MeterTimeSeriesData write(DistributionSummary summary) {
        data.put("count", summary.count());
        data.put("sum", summary.totalAmount());
        data.put("mean", summary.mean());
        data.put("max", summary.max());

        return this;
    }

    public MeterTimeSeriesData write(Meter meter) {
        String type = meter.getId().getType().toString().toLowerCase();
        data.put("type", type);
        return this;
    }

    public static MeterTimeSeriesData of(Meter meter) {
        MeterTimeSeriesData data = new MeterTimeSeriesData();
        data.data.put("id", IDGenerator.RANDOM.generate());
        data.write(meter);
        meter.match(
            data::write,
            data::write,
            data::write,
            data::write,
            data::write,
            data::write,
            data::write,
            data::write,
            data::write);


        return data;
    }
}
