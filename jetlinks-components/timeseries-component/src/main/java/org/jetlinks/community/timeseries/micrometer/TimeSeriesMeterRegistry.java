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

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.StringType;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TimeSeriesMeterRegistry extends StepMeterRegistry implements ThreadFactory  {

    TimeSeriesManager timeSeriesManager;

    TimeSeriesMetric metric;

    AtomicInteger count = new AtomicInteger();

    private final Map<String, String> customTags;

    private final Map<String, DataType> tagDefine;

    public TimeSeriesMeterRegistry(TimeSeriesManager timeSeriesManager,
                                   TimeSeriesMetric metric,
                                   TimeSeriesRegistryProperties config,
                                   Map<String, String> customTags,
                                   String... tagKeys) {
        this(timeSeriesManager,
             metric,
             config,
             customTags,
             Arrays
                 .stream(tagKeys)
                 .collect(Collectors.toMap(Function.identity(), ignore -> StringType.GLOBAL)));
    }

    public TimeSeriesMeterRegistry(TimeSeriesManager timeSeriesManager,
                                   TimeSeriesMetric metric,
                                   TimeSeriesRegistryProperties config,
                                   Map<String, String> customTags,
                                   Map<String, DataType> tagDefine) {
        super(new TimeSeriesPropertiesPropertiesConfigAdapter(config), Clock.SYSTEM);
        this.timeSeriesManager = timeSeriesManager;
        this.metric = metric;
        this.customTags = customTags;

        this.tagDefine = new HashMap<>(tagDefine);
        start(this);
    }


    @Override
    @SuppressWarnings("all")
    public Thread newThread(@Nonnull Runnable r) {
        return new FastThreadLocalThread(
            r,
            "time-series-metric-" + metric.getId() + (count.getAndIncrement() == 0 ? "" : ("-" + count.get())));
    }

    public void reload() {
        timeSeriesManager
            .registerMetadata(MeterTimeSeriesMetadata.of(metric, tagDefine))
            .subscribe(ignore -> {
                       },
                       error -> log.error("register metric[{}] metadata error", metric.getId(), error));
    }

    @Override
    public void start(ThreadFactory threadFactory) {
        super.start(threadFactory);
        reload();
    }

    @Override
    @SneakyThrows
    protected void publish() {
        Flux.fromIterable(this.getMeters())
            .map(meter -> MeterTimeSeriesData
                .of(meter)
                .name(getConventionName(meter.getId()))
                .write(customTags)
                .write(getConventionTags(meter.getId()), (key) -> tagDefine.getOrDefault(key, StringType.GLOBAL)))
            .flatMap(timeSeriesManager.getService(metric)::commit)
            .then()
            .toFuture()
            .get();
    }

    @Override
    public void close() {
        log.info("close micrometer metric [{}]", metric.getId());
        super.close();
    }

    @Override
    @Nonnull
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
