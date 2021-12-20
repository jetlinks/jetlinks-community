package org.jetlinks.community.device.web.protocol;

import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportDetailTest {

    @Test
    void of() {
        TransportDetail.of(new MockProtocolSupport(), DefaultTransport.TCP);
        TransportDetail detail = new TransportDetail("id", "name");
        detail.setId("id");
        detail.setName("name");
        assertNotNull(detail.getId());
        assertNotNull(detail.getName());
    }
}