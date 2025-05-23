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
package org.jetlinks.community.things.data.operations;

import lombok.AllArgsConstructor;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor(staticName = "of")
public class TableSafeMetricBuilder implements MetricBuilder{
    private final MetricBuilder target;

    @Override
    public <K> Optional<K> option(ConfigKey<K> key) {
        return target.option(key);
    }

    @Override
    public String getThingIdProperty() {
        return target.getThingIdProperty();
    }

    @Override
    public String getTemplateIdProperty() {
        return target.getTemplateIdProperty();
    }

    @Override
    public String createEventAllInOneMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
        return ThingsDatabaseUtils.createTableName(target.createEventAllInOneMetric(thingType, thingTemplateId, thingId));
    }

    @Override
    public String createPropertyMetric(@Nonnull String thingType,
                                       @Nonnull String thingTemplateId,
                                       String thingId, String group) {
        return ThingsDatabaseUtils.createTableName(target.createPropertyMetric(thingType, thingTemplateId, thingId, group));
    }

    @Override
    public String createEventMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId, @Nonnull String eventId) {
        return ThingsDatabaseUtils.createTableName(
            target.createEventMetric(thingType, thingTemplateId, thingId, eventId)
        );
    }

    @Override
    public String createLogMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
        return ThingsDatabaseUtils.createTableName(
            target.createLogMetric(thingType, thingTemplateId, thingId)
        );
    }

    @Override
    public String createLogMetric(@Nonnull String thingType) {
        return ThingsDatabaseUtils.createTableName(target.createLogMetric(thingType));
    }

    @Override
    public String createPropertyMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
        return ThingsDatabaseUtils.createTableName(
            target.createPropertyMetric(thingType, thingTemplateId, thingId)
        );
    }

}
