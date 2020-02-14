package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimeSeriesMeterRegistry extends StepMeterRegistry {

    TimeSeriesManager timeSeriesManager;

    TimeSeriesMetric metric;

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("time-series-metrics-publisher");

    private Map<String, String> customTags;


    public TimeSeriesMeterRegistry(TimeSeriesManager timeSeriesManager,
                                   TimeSeriesMetric metric,
                                   TimeSeriesRegistryProperties config,
                                   Map<String, String> customTags) {
        super(new TimeSeriesPropertiesPropertiesConfigAdapter(config), Clock.SYSTEM);
        this.timeSeriesManager = timeSeriesManager;
        this.metric = metric;
        this.customTags = customTags;
        start(DEFAULT_THREAD_FACTORY);
    }

    @Override
    public void start(ThreadFactory threadFactory) {
        super.start(threadFactory);
        timeSeriesManager.registerMetadata(MeterTimeSeriesMetadata.of(metric))
            .doOnError(e -> log.error("register metric metadata error", e))
            .subscribe((r) -> log.error("register metric [{}] metadata success", metric.getId()));
    }

    @Override
    protected void publish() {
        timeSeriesManager
            .getService(metric)
            .save(Flux.fromIterable(this.getMeters())
                .map(meter -> MeterTimeSeriesData.of(meter)
                    .name(getConventionName(meter.getId()))
                    .write(customTags)
                    .write(getConventionTags(meter.getId()))))
            .doOnError(e -> log.error("failed to send metrics [{}]",metric.getId(), e))
            .doOnSuccess(nil -> log.debug("success send metrics [{}]",metric.getId()))
            .subscribe();
    }


    @Override
    @Nonnull
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
