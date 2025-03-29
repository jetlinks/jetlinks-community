package org.jetlinks.community.device.service.data;

import lombok.AllArgsConstructor;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.community.things.data.ThingsDataContext;
import org.jetlinks.community.things.data.ThingsDataCustomizer;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Optional;

@Component
@AllArgsConstructor
public class DeviceThingsDataCustomizer implements ThingsDataCustomizer {

    private final DeviceDataStorageProperties properties;

    @Override
    public void custom(ThingsDataContext context) {

        //兼容之前版本的表名策略
        context.customMetricBuilder(
            ThingsBridgingDeviceDataService.thingType,
            new MetricBuilder() {

                @Override
                public <K> Optional<K> option(ConfigKey<K> key) {
                    Object value = properties.getMetric().getOptions().get(key.getKey());
                    if (value == null) {
                        return Optional.empty();
                    }
                    return Optional.of(key.convertValue(value));
                }

                @Override
                public String getTemplateIdProperty() {
                    return "productId";
                }

                @Override
                public String getThingIdProperty() {
                    return "deviceId";
                }

                @Override
                public String createLogMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
                    return properties.getMetric().getLogPrefix() + thingTemplateId;
//                    return DeviceTimeSeriesMetric.deviceLogMetricId(thingTemplateId);
                }

                @Override
                public String createLogMetric(@Nonnull String thingType) {
                    return properties.getMetric().getLogAllInOne();
                }

                @Override
                public String createPropertyMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
                    return properties.getMetric().getPropertyPrefix() + thingTemplateId;
//                    return DeviceTimeSeriesMetric.devicePropertyMetricId(thingTemplateId);
                }

                @Override
                public String createEventAllInOneMetric(@Nonnull String thingType, @Nonnull String thingTemplateId, String thingId) {
                    return properties.getMetric().getEventPrefix() + thingTemplateId + "_events";

                    // return MetricBuilder.super.createEventAllInOneMetric(thingType, thingTemplateId, thingId);
                }

                @Override
                public String createEventMetric(@Nonnull String thingType,
                                                @Nonnull String thingTemplateId,
                                                String thingId,
                                                @Nonnull String eventId) {
                    return properties.getMetric().getEventPrefix() + thingTemplateId + "_" + eventId;
                    // return DeviceTimeSeriesMetric.deviceEventMetricId(thingTemplateId, eventId);
                }
            }
        );

        context.setDefaultPolicy(properties.getDefaultPolicy());

        context.customSettings(ThingsBridgingDeviceDataService.thingType,
                               properties);

    }
}
