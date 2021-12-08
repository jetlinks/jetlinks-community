package org.jetlinks.community.device.web.protocol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolDetailTest {

    @Test
    void of() {
        ProtocolDetail protocolDetail = new ProtocolDetail("test", "test", new ArrayList<>());
        protocolDetail.setId("test");
        protocolDetail.setName("test");
        TransportDetail transportDetail = new TransportDetail("test", "test");
        transportDetail.setId("test");
        String text = TransportSupportType.DECODE.getText();
        assertNotNull(text);
        String value = TransportSupportType.DECODE.getValue();
        assertNotNull(value);

    }
}