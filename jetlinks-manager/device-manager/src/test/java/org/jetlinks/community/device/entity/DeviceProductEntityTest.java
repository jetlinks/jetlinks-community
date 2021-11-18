package org.jetlinks.community.device.entity;

import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceProductEntityTest {

    @Test
    void getProtocolName() {
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        List<Transport> list = new ArrayList<>();
        list.add(DefaultTransport.WebSocket);
        deviceProductEntity.getTransportEnum(list);
    }
}