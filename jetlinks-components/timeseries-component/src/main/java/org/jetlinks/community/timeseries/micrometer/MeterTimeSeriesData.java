package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.*;
import org.jetlinks.community.timeseries.TimeSeriesData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MeterTimeSeriesData implements TimeSeriesData {

    private Map<String, Object> data = new HashMap<>();

    private long timestamp = System.currentTimeMillis();

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

    public MeterTimeSeriesData write(List<Tag> tags) {
        for (Tag tag : tags) {
            data.put(tag.getKey(), tag.getValue());
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
