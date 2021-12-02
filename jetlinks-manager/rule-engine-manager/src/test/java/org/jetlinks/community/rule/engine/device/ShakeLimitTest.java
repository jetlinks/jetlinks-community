package org.jetlinks.community.rule.engine.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShakeLimitTest {

    @Test
    void wrapReactorQl() {
        ShakeLimit limit = new ShakeLimit();
        limit.setEnabled(false);
        String s = limit.wrapReactorQl("table", "test");
        assertNotNull(s);
        limit.setEnabled(true);
        limit.setTime(1);
        limit.setThreshold(1);
        limit.setAlarmFirst(true);
        String s1 = limit.wrapReactorQl("table", "test");
        assertNotNull(s1);
        assertTrue(limit.isEnabled());


    }

    @Test
    void transfer() {
    }
}