package org.jetlinks.community.device.measurements.status;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.jetlinks.community.dashboard.MeasurementParameter;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesService;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DataType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceStatusRecordMeasurementTest {
    //AggNumberOfOnlineDeviceDimension类
    @Test
    void getValueType(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.AggNumberOfOnlineDeviceDimension dimension = measurement.new AggNumberOfOnlineDeviceDimension();
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);
    }
    @Test
    void getParams(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.AggNumberOfOnlineDeviceDimension dimension = measurement.new AggNumberOfOnlineDeviceDimension();
        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);
        assertEquals("设备型号",params.getProperties().get(0).getName());
    }
    @Test
    void isRealTime(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.AggNumberOfOnlineDeviceDimension dimension = measurement.new AggNumberOfOnlineDeviceDimension();
        boolean realTime = dimension.isRealTime();
        assertFalse(realTime);
    }
    @Test
    void getValue(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);

        Mockito.when(timeSeriesManager.getService(Mockito.any(TimeSeriesMetric.class)))
            .thenReturn(timeSeriesService);
        Mockito.when(timeSeriesService.aggregation(Mockito.any(AggregationQueryParam.class)))
            .thenReturn(Flux.just(AggregationData.of(new HashMap<>())));
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.AggNumberOfOnlineDeviceDimension dimension = measurement.new AggNumberOfOnlineDeviceDimension();
        Map<String, Object> params = new HashMap<>();
        MeasurementParameter parameter = MeasurementParameter.of(params);
        dimension.getValue(parameter).subscribe();
    }

    //CurrentNumberOfDeviceDimension类
    @Test
    void CurrentGetValueType(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.CurrentNumberOfDeviceDimension dimension = measurement.new CurrentNumberOfDeviceDimension();
        DataType valueType = dimension.getValueType();
        assertNotNull(valueType);
    }
    @Test
    void CurrentGetParams(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.CurrentNumberOfDeviceDimension dimension = measurement.new CurrentNumberOfDeviceDimension();
        ConfigMetadata params = dimension.getParams();
        assertNotNull(params);
        assertEquals("设备型号",params.getProperties().get(0).getName());
    }
    @Test
    void CurrentIsRealTime(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.CurrentNumberOfDeviceDimension dimension = measurement.new CurrentNumberOfDeviceDimension();
        boolean realTime = dimension.isRealTime();
        assertFalse(realTime);
    }
    @Test
    void CurrentGetValue(){
        LocalDeviceInstanceService instanceService = Mockito.mock(LocalDeviceInstanceService.class);
        TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
        TimeSeriesService timeSeriesService = Mockito.mock(TimeSeriesService.class);
        ReactiveQuery<DeviceInstanceEntity> query = Mockito.mock(ReactiveQuery.class);

        Mockito.when(instanceService.createQuery())
            .thenReturn(query);
        Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.count())
            .thenReturn(Mono.just(1));

        DeviceStatusRecordMeasurement measurement = new DeviceStatusRecordMeasurement(instanceService,timeSeriesManager);
        DeviceStatusRecordMeasurement.CurrentNumberOfDeviceDimension dimension = measurement.new CurrentNumberOfDeviceDimension();
        Map<String, Object> params = new HashMap<>();
        params.put("productId","productId");
        params.put("state", DeviceState.online);
        MeasurementParameter parameter = MeasurementParameter.of(params);
        dimension.getValue(parameter).subscribe();
    }
}