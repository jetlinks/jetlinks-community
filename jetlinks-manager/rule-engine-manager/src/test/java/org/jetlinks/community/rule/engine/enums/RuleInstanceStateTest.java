package org.jetlinks.community.rule.engine.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleInstanceStateTest {

    @Test
    void getValue() {
         assertNotNull(RuleInstanceState.disable.getValue());
         assertNotNull(RuleInstanceState.disable.getText());
    }

    @Test
    void getText() {
    }
}