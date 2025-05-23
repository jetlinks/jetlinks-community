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

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeDDLOperationsBase;
import reactor.core.publisher.Mono;

import java.util.List;

class ElasticSearchRowModeDDLOperations extends RowModeDDLOperationsBase {

    private final ElasticSearchIndexManager indexManager;

    public ElasticSearchRowModeDDLOperations(String thingType,
                                             String templateId,
                                             String thingId,
                                             DataSettings settings,
                                             MetricBuilder metricBuilder,
                                             ElasticSearchIndexManager indexManager) {
        super(thingType, templateId, thingId, settings, metricBuilder);
        this.indexManager = indexManager;
    }

    @Override
    protected boolean isOnlySupportsOneObjectOrArrayProperty() {
        return true;
    }

    @Override
    protected Mono<Void> register(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(metric, properties));
    }

    @Override
    protected Mono<Void> reload(MetricType metricType, String metric, List<PropertyMetadata> properties) {
        return indexManager
            .putIndex(new DefaultElasticSearchIndexMetadata(metric, properties));
    }
}
