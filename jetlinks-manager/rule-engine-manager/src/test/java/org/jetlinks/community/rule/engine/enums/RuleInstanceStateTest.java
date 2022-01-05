package org.jetlinks.community.rule.engine.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuleInstanceStateTest {

    @Test
    void getValue() {
         assertNotNull(RuleInstanceState.disable.getValue());
         assertNotNull(RuleInstanceState.disable.getText());
    }

}