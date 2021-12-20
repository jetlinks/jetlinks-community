package org.jetlinks.community.device.response;

import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.enums.DeviceType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceInfoTest {

    @Test
    void get() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId("test");
        deviceInfo.setName("test");
        deviceInfo.setProjectId("test");
        deviceInfo.setProjectName("test");
        deviceInfo.setClassifiedId("test");
        deviceInfo.setNetworkWay("test");
        deviceInfo.setProjectId("test");
        deviceInfo.setState(DeviceState.online);
        deviceInfo.setCreatorId("test");
        deviceInfo.setCreatorName("test");
        deviceInfo.setCreateTime(10l);
        deviceInfo.setRegistryTime(10l);
        deviceInfo.setOrgId("test");
        deviceInfo.setDescribe("test");
        deviceInfo.setProductName("test");
        deviceInfo.setDeviceType("test");
        deviceInfo.setTransportProtocol("test");
        deviceInfo.setMessageProtocol("test");
        deviceInfo.setDeriveMetadata("test");
        deviceInfo.setConfiguration(new HashMap<>());
        deviceInfo.setProductId("test");


        String id = deviceInfo.getId();
        assertNotNull(id);
        String name = deviceInfo.getName();
        assertNotNull(name);
        String projectId = deviceInfo.getProjectId();
        assertNotNull(projectId);
        String productId = deviceInfo.getProductId();
        assertNotNull(productId);
        String projectName = deviceInfo.getProjectName();
        assertNotNull(projectName);
        String classifiedId = deviceInfo.getClassifiedId();
        assertNotNull(classifiedId);
        String networkWay = deviceInfo.getNetworkWay();
        assertNotNull(networkWay);
        DeviceState state = deviceInfo.getState();
        assertNotNull(state);
        String creatorId = deviceInfo.getCreatorId();
        assertNotNull(creatorId);
        String creatorName = deviceInfo.getCreatorName();
        assertNotNull(creatorName);
        Long createTime = deviceInfo.getCreateTime();
        assertNotNull(createTime);
        Long registryTime = deviceInfo.getRegistryTime();
        assertNotNull(registryTime);
        String orgId = deviceInfo.getOrgId();
        assertNotNull(orgId);
        String deviceType = deviceInfo.getDeviceType();
        assertNotNull(deviceType);
        String transportProtocol = deviceInfo.getTransportProtocol();
        assertNotNull(transportProtocol);
        String messageProtocol = deviceInfo.getMessageProtocol();
        assertNotNull(messageProtocol);
        String describe = deviceInfo.getDescribe();
        assertNotNull(describe);
        String productName = deviceInfo.getProductName();
        assertNotNull(productName);
        String deriveMetadata = deviceInfo.getDeriveMetadata();
        assertNotNull(deriveMetadata);
        Map<String, Object> configuration = deviceInfo.getConfiguration();
        assertNotNull(configuration);
        new DeviceInfo(
            "test", "test", "test", "test", "test", "test", "test", DeviceState.online,
            "test", "test", 10l, 10l, "test", "test", "test", "test",
            "test", "test", "test", new HashMap<>()
        );
        DeviceInfo.builder()
            .id("test")
            .build();
    }

    @Test
    void of(){
        DeviceInstanceEntity instanceEntity = new DeviceInstanceEntity();
        Map<String, Object> map = new HashMap<>();
        map.put("test","test");
//        instanceEntity.setConfiguration(map);
//        instanceEntity.setDeriveMetadata("test");
        DeviceProductEntity productEntity = new DeviceProductEntity();
        productEntity.setMessageProtocol("test");
        productEntity.setTransportProtocol("test");
        productEntity.setDeviceType(DeviceType.device);
        productEntity.setClassifiedId("test");
        productEntity.setProjectId("test");
        productEntity.setProjectName("test");
        productEntity.setName("test");
        productEntity.setNetworkWay("test");
        productEntity.setMetadata("test");
        productEntity.setConfiguration(map);
        DeviceInfo of = DeviceInfo.of(instanceEntity, productEntity);
        String name = of.getProductName();
        assertNotNull(name);
    }
}