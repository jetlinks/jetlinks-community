package org.jetlinks.community.rule.engine.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActionTest {

    @Test
    void getExecutor() {

        Action action = new Action();
        action.setExecutor("test");
        action.setConfiguration(new HashMap<>());
        assertNotNull(action.getExecutor());
        assertNotNull(action.getConfiguration());
    }
}