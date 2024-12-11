package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeSaveOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.jetlinks.community.things.data.ThingsDataConstants.COLUMN_MESSAGE_ID;


class TDengineRowModeSaveOperations extends RowModeSaveOperationsBase {
    private final TDengineThingDataHelper helper;

    public TDengineRowModeSaveOperations(ThingsRegistry registry,
                                         MetricBuilder metricBuilder,
                                         DataSettings settings,
                                         TDengineThingDataHelper helper) {
        super(registry, metricBuilder, settings);
        this.helper = helper;
    }

    protected boolean isTagValue(String metric,
                                 String key,
                                 Object value) {
        return Objects.equals(metricBuilder.getThingIdProperty(), key)
            || Objects.equals(ThingsDataConstants.COLUMN_PROPERTY_ID, key);
    }
    static Set<String> IGNORE_COLUMN = new HashSet<>(Arrays.asList(
        ThingsDataConstants.COLUMN_ID,
        ThingsDataConstants.COLUMN_PROPERTY_OBJECT_VALUE,
        ThingsDataConstants.COLUMN_PROPERTY_ARRAY_VALUE,
        ThingsDataConstants.COLUMN_PROPERTY_GEO_VALUE,
        ThingsDataConstants.COLUMN_PROPERTY_TIME_VALUE,
        ThingsDataConstants.COLUMN_TIMESTAMP
    ));

    @Override
    protected String createPropertyDataId(String property, ThingMessage message, long timestamp) {
        return String.valueOf(timestamp);
    }

    @Override
    protected Map<String, Object> createRowPropertyData(String id,
                                                        long timestamp,
                                                        ThingMessage message,
                                                        PropertyMetadata property,
                                                        Object value) {
        Map<String, Object> data =  super.createRowPropertyData(id, timestamp, message, property, value);
        IGNORE_COLUMN.forEach(data::remove);
        return data;
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {

        return helper.doSave(metric, data, this::isTagValue);
    }

    @Override
    protected Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        return helper.doSave(metric, data, this::isTagValue);
    }

    @Override
    protected Map<String, Object> createLogData(String templateId, ThingMessage message) {
        Map<String, Object> data = super.createLogData(templateId,message);
        data.put(COLUMN_MESSAGE_ID, Objects.isNull(message.getMessageId()) ? "" : message.getMessageId());
        return data;
    }
}
