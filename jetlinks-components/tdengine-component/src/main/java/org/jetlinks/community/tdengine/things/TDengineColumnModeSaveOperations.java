package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.things.data.ThingsDataConstants;
import org.jetlinks.community.things.data.operations.ColumnModeSaveOperationsBase;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.jetlinks.community.things.data.ThingsDataConstants.COLUMN_MESSAGE_ID;


class TDengineColumnModeSaveOperations extends ColumnModeSaveOperationsBase {
    private final TDengineThingDataHelper helper;

    public TDengineColumnModeSaveOperations(ThingsRegistry registry,
                                            MetricBuilder metricBuilder,
                                            DataSettings settings,
                                            TDengineThingDataHelper helper) {
        super(registry, metricBuilder, settings);
        this.helper = helper;
    }

    static Set<String> IGNORE_COLUMN = new HashSet<>(Arrays.asList(
        ThingsDataConstants.COLUMN_ID,
        ThingsDataConstants.COLUMN_TIMESTAMP
    ));

    @Override
    protected Map<String, Object> createLogData(String templateId, ThingMessage message) {
        Map<String, Object> data = super.createLogData(templateId,message);
        data.put(COLUMN_MESSAGE_ID, Objects.isNull(message.getMessageId()) ? "" : message.getMessageId());
        return data;
    }
    @Override
    protected String createPropertyDataId(ThingMessage message) {
        return message.getMessageId();
    }

    @Override
    protected Map<String, Object> handlePropertiesData(ThingMetadata metadata, Map<String, Object> properties) {
        properties = super.handlePropertiesData(metadata, properties);
        IGNORE_COLUMN.forEach(properties::remove);
        return properties;
    }

    protected boolean isTagValue(String metric,
                                 String key,
                                 Object value) {
        return Objects.equals(metricBuilder.getThingIdProperty(), key);
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {

        return helper.doSave(metric, data, this::isTagValue);
    }

    @Override
    protected Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        return helper.doSave(metric, data, this::isTagValue);
    }
}
