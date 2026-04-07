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
package org.jetlinks.community.micrometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.noop.*;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

public class NoopMeterRegistry extends MeterRegistry {
    public static final NoopMeterRegistry INSTANCE = new NoopMeterRegistry();

    private NoopMeterRegistry() {
        super(Clock.SYSTEM);
    }

    @Override
    @Nonnull
    protected <T> Gauge newGauge(@Nonnull Meter.Id id, T t, @Nonnull ToDoubleFunction<T> toDoubleFunction) {
        return new NoopGauge(id);
    }

    @Override
    @Nonnull
    protected Counter newCounter(@Nonnull Meter.Id id) {
        return new NoopCounter(id);
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        return new NoopLongTaskTimer(id);
    }

    @Override
    protected <T> TimeGauge newTimeGauge(Meter.Id id, T obj, TimeUnit valueFunctionUnit, ToDoubleFunction<T> valueFunction) {
        return new NoopTimeGauge(id);
    }

    @Override
    protected LongTaskTimer newLongTaskTimer(Meter.Id id) {
        return new NoopLongTaskTimer(id);
    }

    @Override
    @Nonnull
    protected Timer newTimer(@Nonnull Meter.Id id, @Nonnull DistributionStatisticConfig distributionStatisticConfig, @Nonnull PauseDetector pauseDetector) {
        return new NoopTimer(id);
    }

    @Override
    @Nonnull
    protected DistributionSummary newDistributionSummary(@Nonnull Meter.Id id, @Nonnull DistributionStatisticConfig distributionStatisticConfig, double v) {
        return new NoopDistributionSummary(id);
    }

    @Override
    @Nonnull
    protected Meter newMeter(@Nonnull Meter.Id id, @Nonnull Meter.Type type, @Nonnull Iterable<Measurement> iterable) {
        return new NoopMeter(id);
    }

    @Override
    @Nonnull
    protected <T> FunctionTimer newFunctionTimer(@Nonnull Meter.Id id, @Nonnull T t,
                                                 @Nonnull ToLongFunction<T> toLongFunction,
                                                 @Nonnull ToDoubleFunction<T> toDoubleFunction,
                                                 @Nonnull TimeUnit timeUnit) {
        return new NoopFunctionTimer(id);
    }

    @Override
    @Nonnull
    protected <T> FunctionCounter newFunctionCounter(@Nonnull Meter.Id id, @Nonnull T t, @Nonnull ToDoubleFunction<T> toDoubleFunction) {
        return new NoopFunctionCounter(id);
    }

    @Override
    @Nonnull
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    @Nonnull
    protected DistributionStatisticConfig defaultHistogramConfig() {
        return DistributionStatisticConfig.NONE;
    }
}
