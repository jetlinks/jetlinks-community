package org.jetlinks.community.network.manager.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigStateTest {

    @Test
    void getText() {
        assertNotNull(NetworkConfigState.disabled.getText());
        assertNotNull(NetworkConfigState.disabled.getValue());
    }
}