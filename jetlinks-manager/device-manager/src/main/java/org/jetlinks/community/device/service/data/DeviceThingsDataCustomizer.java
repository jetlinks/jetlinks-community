package org.jetlinks.community.device.service.data;

import lombok.AllArgsConstructor;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.things.data.ThingsDataContext;
import org.jetlinks.community.things.data.ThingsDataCustomizer;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.springframework.stereotype.Component;

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
                public String getThingIdProperty() {
                    return "deviceId";
                }

                @Override
                public String createLogMetric(String thingType, String thingTemplateId, String thingId) {
                    return DeviceTimeSeriesMetric.deviceLogMetricId(thingTemplateId);
                }

                @Override
                public String createPropertyMetric(String thingType, String thingTemplateId, String thingId) {
                    return DeviceTimeSeriesMetric.devicePropertyMetricId(thingTemplateId);
                }

                @Override
                public String createEventAllInOneMetric(String thingType, String thingTemplateId, String thingId) {
                    return MetricBuilder.super.createEventAllInOneMetric(thingType, thingTemplateId, thingId);
                }

                @Override
                public String createEventMetric(String thingType, String thingTemplateId, String thingId, String eventId) {
                    return DeviceTimeSeriesMetric.deviceEventMetricId(thingTemplateId, eventId);
                }
            }
        );

        context.setDefaultPolicy(properties.getDefaultPolicy());

        context.customSettings(ThingsBridgingDeviceDataService.thingType,
                               properties);

    }
}
