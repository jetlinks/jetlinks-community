package org.jetlinks.community.things.data;

import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;

public interface ThingsDataContext {

    void customMetricBuilder(String thingType, MetricBuilder metricBuilder);

    void customSettings(String thingType, DataSettings settings);

    void setDefaultPolicy(String policy);

    void setDefaultSettings(DataSettings settings);

    void addPolicy(ThingsDataRepositoryStrategy policy);

}
