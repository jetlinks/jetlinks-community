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
package org.jetlinks.community.timescaledb.thing;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.operations.ColumnModeSaveOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeSaveOperationsBase;
import org.jetlinks.community.timescaledb.TimescaleDBDataWriter;
import org.jetlinks.community.timescaledb.TimescaleDBUtils;
import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TimescaleDBColumnModeSaveOperations extends ColumnModeSaveOperationsBase {

    private final TimescaleDBDataWriter writer;

    public TimescaleDBColumnModeSaveOperations(ThingsRegistry registry,
                                               MetricBuilder metricBuilder,
                                               DataSettings settings,
                                               TimescaleDBDataWriter writer) {
        super(registry, metricBuilder, settings);
        this.writer = writer;
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {
        return writer.save(TimescaleDBUtils.getTableName(metric), data.getData());
    }

    @Override
    protected Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        return writer.save(TimescaleDBUtils.getTableName(metric), data.map(TimeSeriesData::getData));
    }
}
