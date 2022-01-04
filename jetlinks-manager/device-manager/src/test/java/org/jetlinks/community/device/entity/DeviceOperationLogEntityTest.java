package org.jetlinks.community.device.entity;

import org.jetlinks.community.device.enums.DeviceLogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceOperationLogEntityTest {

    @Test
    void get() {
        DeviceOperationLogEntity entity = new DeviceOperationLogEntity();
        entity.setDeviceId("test");
        String deviceId = entity.getDeviceId();
        assertNotNull(deviceId);
        new DeviceOperationLogEntity("test", "test", "test", DeviceLogType.child, 10l, "test", "test", 10l, "test");
        DeviceOperationLogEntity build = DeviceOperationLogEntity.builder()
            .id("test")
            .deviceId("test")
            .productId("test")
            .type(DeviceLogType.child)
            .createTime(10l)
            .content("test")
            .orgId("test")
            .timestamp(10l)
            .messageId("test")
            .build();
        assertNotNull(build);

    }
}