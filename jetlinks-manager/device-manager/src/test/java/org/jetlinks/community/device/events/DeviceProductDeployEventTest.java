package org.jetlinks.community.device.events;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceProductDeployEventTest {

  @Test
  void get() {
      DeviceProductDeployEvent deviceProductDeployEvent = new DeviceProductDeployEvent();
      deviceProductDeployEvent.setId("test");
      deviceProductDeployEvent.setName("test");
      deviceProductDeployEvent.setProjectId("test");
      deviceProductDeployEvent.setProjectName("test");
      deviceProductDeployEvent.setDescribe("test");
      deviceProductDeployEvent.setDeviceType("test");
      deviceProductDeployEvent.setClassifiedId("test");
      deviceProductDeployEvent.setMessageProtocol("test");
      deviceProductDeployEvent.setTransportProtocol("test");
      deviceProductDeployEvent.setNetworkWay("test");
      deviceProductDeployEvent.setDeviceType("test");
      deviceProductDeployEvent.setConfiguration(new HashMap<>());
      deviceProductDeployEvent.setState((byte)1);
      deviceProductDeployEvent.setCreateTime( 1l);
      deviceProductDeployEvent.setOrgId("test");
      String id = deviceProductDeployEvent.getId();
      assertNotNull(id);
      String name = deviceProductDeployEvent.getName();
      assertNotNull(name);
      String projectId = deviceProductDeployEvent.getProjectId();
      assertNotNull(projectId);
      String projectName = deviceProductDeployEvent.getProjectName();
      assertNotNull(projectName);
      String describe = deviceProductDeployEvent.getDescribe();
      assertNotNull(describe);
      String classifiedId = deviceProductDeployEvent.getClassifiedId();
      assertNotNull(classifiedId);
      String messageProtocol = deviceProductDeployEvent.getMessageProtocol();
      assertNotNull(messageProtocol);
      String transportProtocol = deviceProductDeployEvent.getTransportProtocol();
      assertNotNull(transportProtocol);
      String networkWay = deviceProductDeployEvent.getNetworkWay();
      assertNotNull(networkWay);
      String deviceType = deviceProductDeployEvent.getDeviceType();
      assertNotNull(deviceType);
      Map<String, Object> configuration = deviceProductDeployEvent.getConfiguration();
      assertNotNull(configuration);
      Byte state = deviceProductDeployEvent.getState();
      assertNotNull(state);
      Long createTime = deviceProductDeployEvent.getCreateTime();
      assertNotNull(createTime);
      String orgId = deviceProductDeployEvent.getOrgId();
      assertNotNull(orgId);


  }
}