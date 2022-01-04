package org.jetlinks.community.device.response;

import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceAllInfoResponseTest {

    @Test
    void get() {
        DeviceAllInfoResponse response = new DeviceAllInfoResponse();
        response.setDeviceInfo(new DeviceInfo());
        response.setRealState(DeviceState.online);
        response.setOfflineTime(10L);
        response.setOnlineTime(10L);
        response.setProperties(new HashMap<>());
        response.setEventCounts(new HashMap<>());

        DeviceInfo deviceInfo = response.getDeviceInfo();
        assertNotNull(deviceInfo);
        DeviceState realState = response.getRealState();
        assertNotNull(realState);
        long onlineTime = response.getOnlineTime();
        assertEquals(10L,onlineTime);
        long offlineTime = response.getOfflineTime();
        assertEquals(10L,offlineTime);
        Map<String, Object> properties = response.getProperties();
        assertNotNull(properties);
        Map<String, Integer> eventCounts = response.getEventCounts();
        assertNotNull(eventCounts);
    }

    @Test
    void of(){
        DeviceRunInfo deviceRunInfo = new DeviceRunInfo();
        deviceRunInfo.setOnlineTime(10l);
        deviceRunInfo.setOfflineTime(10l);
        deviceRunInfo.setState(DeviceState.online);
        DeviceAllInfoResponse of = DeviceAllInfoResponse.of(new DeviceInfo(), deviceRunInfo);
        assertNotNull(of);
    }

    @Test
    void ofProperties(){
        DeviceAllInfoResponse response = new DeviceAllInfoResponse();

        DevicePropertiesEntity entity = new DevicePropertiesEntity();
        entity.setProperty("test");
        entity.setFormatValue("test");
        List<DevicePropertiesEntity> properties = new ArrayList<>();
        DeviceAllInfoResponse deviceAllInfoResponse = response.ofProperties(properties);
        assertNotNull(deviceAllInfoResponse);
    }
    @Test
    void ofEventCounts(){
        DeviceAllInfoResponse response = new DeviceAllInfoResponse();
        Map<String, Integer> eventCounts = new HashMap<>();
        DeviceAllInfoResponse deviceAllInfoResponse = response.ofEventCounts(eventCounts);
        assertNotNull(deviceAllInfoResponse);
    }
}