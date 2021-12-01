package org.jetlinks.community.rule.engine.service;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DebugMessageTest {


    @Test
    void of() {
        DebugMessage debugMessage = new DebugMessage();
        debugMessage.setContextId("test");
        debugMessage.setMessage("test");
        debugMessage.setTimestamp(new Date());
        debugMessage.setType("test");
        assertNotNull(debugMessage.getContextId());
        assertNotNull(debugMessage.getType());
        assertNotNull(debugMessage.getTimestamp());
        assertNotNull(debugMessage.getMessage());
        DebugMessage of = DebugMessage.of("test", "test", "test");
        assertNotNull(of.getContextId());
        assertNotNull(of.getType());
        assertNotNull(of.getTimestamp());
        assertNotNull(of.getMessage());
    }
}