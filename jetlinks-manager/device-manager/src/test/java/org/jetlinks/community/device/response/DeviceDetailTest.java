package org.jetlinks.community.device.response;

import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class DeviceDetailTest {

    @Test
    void get() {
        DeviceDetail deviceDetail = new DeviceDetail();
        DeviceTagEntity deviceTagEntity = new DeviceTagEntity();
        deviceTagEntity.setKey("test");
        deviceTagEntity.setValue("test");
        deviceTagEntity.setDeviceId("test");
        List<DeviceTagEntity> tags = new ArrayList<>();
        tags.add(deviceTagEntity);
        deviceDetail.with(tags);
        deviceDetail.with(tags);
        DeviceProductEntity entity = null;
        deviceDetail.with(entity);

        DeviceInstanceEntity device = new DeviceInstanceEntity();
        device.setId("test");
        device.setName("test");
        device.setState(DeviceState.online);
        device.setOrgId("test");
        device.setParentId("test");
        device.setDescribe("test");
        device.setRegistryTime(10l);
        device.setCreateTime(10l);
        Map<String, Object> map = new HashMap<>();
        map.put("test","test");
        device.setConfiguration(map);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("test","test");
        deviceDetail.setConfiguration(map1);
        deviceDetail.with(device);
    }
}