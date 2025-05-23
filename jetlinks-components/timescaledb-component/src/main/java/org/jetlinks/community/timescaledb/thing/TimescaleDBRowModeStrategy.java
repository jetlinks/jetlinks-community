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
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;
import org.jetlinks.community.things.data.operations.TableSafeMetricBuilder;
import org.jetlinks.community.timescaledb.TimescaleDBOperations;

public class TimescaleDBRowModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TimescaleDBOperations operations;
    private final TimescaleDBThingsDataProperties properties;

    public TimescaleDBRowModeStrategy(ThingsRegistry registry,
                                      TimescaleDBOperations operations,
                                      TimescaleDBThingsDataProperties properties) {
        this.registry = registry;
        this.operations = operations;
        this.properties = properties;
    }

    @Override
    public String getId() {
        return "timescaledb-row";
    }

    @Override
    public String getName() {
        return "TimescaleDB-单列模式";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new TimescaleDBRowModeSaveOperations(
            registry,
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            context.getSettings(),
            operations.writer());
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TimescaleDBRowModeQueryOperations(
            thingType,
            templateId,
            thingId,
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            context.getSettings(),
            registry,
            operations.database());
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TimescaleDBRowModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            TableSafeMetricBuilder.of(context.getMetricBuilder()),
            operations.database(),
            properties);
    }

    @Override
    public int getOrder() {
        return 10060;
    }
}
