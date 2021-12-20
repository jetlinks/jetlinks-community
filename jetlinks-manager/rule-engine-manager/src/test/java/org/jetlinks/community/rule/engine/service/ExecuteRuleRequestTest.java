package org.jetlinks.community.rule.engine.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExecuteRuleRequestTest {

    @Test
    void get() {
        ExecuteRuleRequest request = new ExecuteRuleRequest();
        request.setContextId("test");
        request.setData("test");
        request.setEndWith("test");
        request.setStartWith("test");
        request.setSessionId("test");

        assertNotNull(request.getContextId());
        assertNotNull(request.getData());
        assertNotNull(request.getEndWith());
        assertNotNull(request.getStartWith());
        assertNotNull(request.getSessionId());
    }
}