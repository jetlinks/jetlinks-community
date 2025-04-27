package org.jetlinks.community.things.data;

import lombok.AllArgsConstructor;
import org.jetlinks.community.things.data.operations.MetricBuilder;
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
