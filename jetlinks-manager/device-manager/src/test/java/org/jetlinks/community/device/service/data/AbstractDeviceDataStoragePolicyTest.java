package org.jetlinks.community.device.service.data;

import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

//class AbstractDeviceDataStoragePolicyTest {
//
//  @Test
//  void saveDeviceMessage() {
//      DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
//      TimeSeriesManager timeSeriesManager = Mockito.mock(TimeSeriesManager.class);
//      DeviceDataStorageProperties dataStorageProperties = Mockito.mock(DeviceDataStorageProperties.class);
//      AbstractDeviceDataStoragePolicy policy = new TimeSeriesColumnDeviceDataStoragePolicy(registry,timeSeriesManager,dataStorageProperties);
//
//
//      policy.saveDeviceMessage();
//  }
//
//  @Test
//  void testSaveDeviceMessage() {
//  }
//}