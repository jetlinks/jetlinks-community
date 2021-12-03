package org.jetlinks.community.device.measurements.status;

import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.dashboard.MeasurementValue;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceStatusChangeMeasurementTest {

    @Test
    void getValueType() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.CountDeviceStateDimension dimension = measurement.new CountDeviceStateDimension();
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);
        assertEquals(DeviceStatusChangeMeasurement.historyValueType, valueType);
    }

    @Test
    void getParams() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.CountDeviceStateDimension dimension = measurement.new CountDeviceStateDimension();
        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);
        assertEquals("周期", params.getProperties().get(0).getName());
    }

    @Test
    void isRealTime() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.CountDeviceStateDimension dimension = measurement.new CountDeviceStateDimension();
        boolean realTime = dimension.isRealTime();
        assertFalse(realTime);
    }

    @Test
    void getValue() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(timeSeriesService);
        Mockito.when(timeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(new HashMap<>())));

        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.CountDeviceStateDimension dimension = measurement.new CountDeviceStateDimension();

        Map<String, Object> params = new HashMap<>();
        MeasurementParameter parameter = MeasurementParameter.of(params);
        dimension.getValue(parameter).subscribe();
        Map<String, Object> map = new HashMap<>();
        map.put("time", "2001年02月03日");
        Mockito.when(timeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(map)));
        dimension.getValue(parameter).subscribe();
    }

    //RealTimeDeviceStateDimension类
    @Test
    void realGetValueType() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.RealTimeDeviceStateDimension dimension = measurement.new RealTimeDeviceStateDimension();
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);
    }

    @Test
    void realGetParams() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.RealTimeDeviceStateDimension dimension = measurement.new RealTimeDeviceStateDimension();
        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);
        assertEquals("设备", params.getProperties().get(0).getName());
    }

    @Test
    void realIsRealTime() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, new BrokerEventBus());
        DeviceStatusChangeMeasurement.RealTimeDeviceStateDimension dimension = measurement.new RealTimeDeviceStateDimension();
        boolean realTime = dimension.isRealTime();
        assertTrue(realTime);
    }

    @Test
    void realGetValue() {
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        EventBus eventBus = Mockito.mock(EventBus.class);

        DeviceStatusChangeMeasurement measurement = new DeviceStatusChangeMeasurement(timeSeriesManager, eventBus);
        DeviceStatusChangeMeasurement.RealTimeDeviceStateDimension dimension = measurement.new RealTimeDeviceStateDimension();
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId", "test");
        MeasurementParameter parameter = MeasurementParameter.of(params);
        DeviceOnlineMessage message = new DeviceOnlineMessage();
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class), Mockito.any(Class.class)))
            .thenReturn(Flux.just(message));
        dimension.getValue(parameter).subscribe(MeasurementValue::getValue);

        ReadPropertyMessage readPropertyMessage = new ReadPropertyMessage();
        readPropertyMessage.setDeviceId("test");
        dimension.createStateValue(readPropertyMessage);

    }
}