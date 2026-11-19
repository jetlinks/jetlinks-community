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
package org.jetlinks.community.iotdb.things;

import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AggregationRequest;
import org.jetlinks.community.things.data.operations.ColumnModeQueryOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

final class IoTDBColumnModeQueryOperations extends ColumnModeQueryOperationsBase {

    private final IoTDBThingDataHelper helper;

    IoTDBColumnModeQueryOperations(String thingType,
                                   String templateId,
                                   String thingId,
                                   MetricBuilder metricBuilder,
                                   DataSettings settings,
                                   ThingsRegistry registry,
                                   IoTDBThingDataHelper helper) {
        super(thingType, templateId, thingId, metricBuilder, settings, registry);
        this.helper = helper;
    }

    @Override
    protected Flux<TimeSeriesData> doQuery(String metric, Query<?, QueryParamEntity> query) {
        return helper.doQuery(metric, query.getParam());
    }

    @Override
    protected <T> Mono<PagerResult<T>> doQueryPage(String metric,
                                                   Query<?, QueryParamEntity> query,
                                                   Function<TimeSeriesData, T> mapper) {
        return helper.doQueryPage(metric, query.getParam(), mapper);
    }

    @Override
    protected Flux<AggregationData> doAggregation(String metric,
                                                  AggregationRequest request,
                                                  AggregationContext context) {
        return helper.aggregate(metric,
                                request.getFilter(),
                                request.getFrom(),
                                request.getTo(),
                                request.getInterval(),
                                request.getFormat(),
                                request.getLimit(),
                                context.getProperties(),
                                false);
    }
}
