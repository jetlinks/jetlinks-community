package org.jetlinks.community.network.manager.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceGatewayEntityTest {


    @Test
    void setName() {
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setName("test");
        entity.setDescribe("test");

        assertNotNull(entity.getName());
        assertNotNull(entity.getDescribe());
    }
}