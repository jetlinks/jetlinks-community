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
package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.DDLOperations;
import org.jetlinks.community.things.data.operations.QueryOperations;
import org.jetlinks.community.things.data.operations.SaveOperations;
import reactor.core.publisher.Flux;

public class TDengineRowModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final TDengineThingDataHelper helper;

    public TDengineRowModeStrategy(ThingsRegistry registry, TDengineThingDataHelper helper) {
        this.registry = registry;
        this.helper = helper;
    }


    @Override
    public String getId() {
        return "tdengine-row";
    }

    @Override
    public String getName() {
        return "TDengine-行式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new TDengineRowModeSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            helper);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineRowModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            helper);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new TDengineRowModeQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            helper);
    }

    @Override
    public Flux<Feature> getFeatures() {
        //事件不支持新增以及修改
        return Flux.just(MetadataFeature.eventNotInsertable,
                         MetadataFeature.eventNotModifiable
        );
    }

    @Override
    public int getOrder() {
        return 10210;
    }

}
