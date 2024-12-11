package org.jetlinks.community.things.data.operations;

import org.jetlinks.community.things.data.ThingsDataConstants;

public interface MetricBuilder {

    MetricBuilder DEFAULT = new MetricBuilder() {
    };


    default String getThingIdProperty() {
        return ThingsDataConstants.COLUMN_THING_ID;
    }

    default String createLogMetric(String thingType,
                                   String thingTemplateId,
                                   String thingId) {
        return thingType + "_log_" + thingTemplateId;
    }

    default String createPropertyMetric(String thingType,
                                        String thingTemplateId,
                                        String thingId) {
        return thingType + "_properties_" + thingTemplateId;
    }

    default String createEventAllInOneMetric(String thingType,
                                             String thingTemplateId,
                                             String thingId) {
        return thingType + "_event_" + thingTemplateId + "_events";
    }

    default String createEventMetric(String thingType,
                                     String thingTemplateId,
                                     String thingId,
                                     String eventId) {
        return thingType + "_event_" + thingTemplateId + "_" + eventId;
    }

    default String getTemplateIdProperty() {
        return ThingsDataConstants.COLUMN_TEMPLATE_ID;
    }
}
