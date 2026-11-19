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

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeDDLOperationsBase;
import reactor.core.publisher.Mono;

import java.util.List;

final class IoTDBRowModeDDLOperations extends RowModeDDLOperationsBase {

    private final IoTDBThingDataHelper helper;
    private final String thingIdColumn;

    IoTDBRowModeDDLOperations(String thingType,
                              String templateId,
                              String thingId,
                              DataSettings settings,
                              MetricBuilder metricBuilder,
                              IoTDBThingDataHelper helper) {
        super(thingType, templateId, thingId, settings, metricBuilder);
        this.helper = helper;
        this.thingIdColumn = metricBuilder.getThingIdProperty();
    }

    @Override
    protected Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return helper.registerMetric(metricType, metric, properties, true, thingIdColumn);
    }

    @Override
    protected Mono<Void> reload(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return helper.registerMetric(metricType, metric, properties, true, thingIdColumn);
    }
}
