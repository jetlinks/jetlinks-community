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

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;

public class IoTDBColumnModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final IoTDBThingDataHelper helper;

    public IoTDBColumnModeStrategy(ThingsRegistry registry, IoTDBThingDataHelper helper) {
        this.registry = registry;
        this.helper = helper;
    }

    @Override
    public String getId() {
        return "iotdb-column";
    }

    @Override
    public String getName() {
        return "IoTDB-列式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new IoTDBColumnModeSaveOperations(registry, context.getMetricBuilder(), context.getSettings(), helper);
    }

    @Override
    protected QueryOperations createForQuery(String thingType,
                                             String templateId,
                                             String thingId,
                                             OperationsContext context) {
        return new IoTDBColumnModeQueryOperations(thingType,
                                                  templateId,
                                                  thingId,
                                                  context.getMetricBuilder(),
                                                  context.getSettings(),
                                                  registry,
                                                  helper);
    }

    @Override
    protected DDLOperations createForDDL(String thingType,
                                         String templateId,
                                         String thingId,
                                         OperationsContext context) {
        return new IoTDBColumnModeDDLOperations(thingType,
                                                templateId,
                                                thingId,
                                                context.getSettings(),
                                                context.getMetricBuilder(),
                                                helper);
    }

    @Override
    public int getOrder() {
        return 10240;
    }
}
