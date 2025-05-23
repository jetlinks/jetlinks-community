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
package org.jetlinks.community.elastic.search.things;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.AbstractThingDataRepositoryStrategy;
import org.jetlinks.community.things.data.operations.*;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public class ElasticSearchRowModeStrategy extends AbstractThingDataRepositoryStrategy {

    private final ThingsRegistry registry;
    private final ElasticSearchService searchService;
    private final AggregationService aggregationService;
    private final ElasticSearchIndexManager indexManager;

    @Override
    public String getId() {
        return "default-row";
    }

    @Override
    public String getName() {
        return "ElasticSearch-行式存储";
    }

    @Override
    public SaveOperations createOpsForSave(OperationsContext context) {
        return new ElasticSearchRowModeSaveOperations(
            registry,
            context.getMetricBuilder(),
            context.getSettings(),
            searchService);
    }

    @Override
    protected QueryOperations createForQuery(String thingType, String templateId, String thingId, OperationsContext context) {
        return new ElasticSearchRowModeQueryOperations(
            thingType,
            templateId,
            thingId,
            context.getMetricBuilder(),
            context.getSettings(),
            registry,
            searchService,
            aggregationService);
    }

    @Override
    protected DDLOperations createForDDL(String thingType, String templateId, String thingId, OperationsContext context) {
        return new ElasticSearchRowModeDDLOperations(
            thingType,
            templateId,
            thingId,
            context.getSettings(),
            context.getMetricBuilder(),
            indexManager);
    }

    @Override
    public int getOrder() {
        return 10000;
    }
}
