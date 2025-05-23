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
package org.jetlinks.community.things.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.community.things.data.operations.*;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

public interface ThingsDataRepositoryStrategy extends Ordered {

    String getId();

    String getName();

    SaveOperations opsForSave(OperationsContext context);

    ThingOperations opsForThing(String thingType, String templateId, String thingId, OperationsContext context);

    TemplateOperations opsForTemplate(String thingType, String templateId, OperationsContext context);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    default Flux<Feature> getFeatures() {
        return Flux.empty();
    }

    @EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
    @Getter
    @AllArgsConstructor
    class OperationsContext {
        public static final OperationsContext DEFAULT = new OperationsContext(MetricBuilder.DEFAULT, new DataSettings());

        private final MetricBuilder metricBuilder;
        private final DataSettings settings;

        public OperationsContext settings(DataSettings settings) {
            return new OperationsContext(metricBuilder, settings);
        }

        public OperationsContext metricBuilder(MetricBuilder metricBuilder) {
            return new OperationsContext(metricBuilder, settings);
        }

    }
}
