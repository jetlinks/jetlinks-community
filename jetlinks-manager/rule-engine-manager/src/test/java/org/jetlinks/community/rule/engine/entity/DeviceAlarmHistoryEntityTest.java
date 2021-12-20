package org.jetlinks.community.rule.engine.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceAlarmHistoryEntityTest {

    @Test
    void setProductId() {
        DeviceAlarmHistoryEntity entity = new DeviceAlarmHistoryEntity();
        entity.setProductId("test");
        entity.setProductName("test");
        entity.setDeviceId("test");
        entity.setDeviceName("test");
        entity.setAlarmId("test");
        entity.setAlarmName("test");
        entity.setAlarmTime(new Date());
        entity.setAlarmData(new HashMap<>());
        entity.setState("test");
        entity.setUpdateTime(new Date());
        entity.setDescription("test");

        assertNotNull(entity.getProductId());
        assertNotNull(entity.getProductName());
        assertNotNull(entity.getDeviceId());
        assertNotNull(entity.getDeviceName());
        assertNotNull(entity.getAlarmId());
        assertNotNull(entity.getAlarmName());
        assertNotNull(entity.getAlarmTime());
        assertNotNull(entity.getAlarmData());
        assertNotNull(entity.getState());
        assertNotNull(entity.getUpdateTime());
        assertNotNull(entity.getDescription());


    }

    @Test
    void setProductName() {
        DeviceAlarmEntity entity = new DeviceAlarmEntity();
        Executable executable = ()-> entity.toRuleInstance();
        assertThrows(IllegalArgumentException.class,executable);
    }
}