package org.jetlinks.community.auth.entity;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MenuViewTest {

    @Test
    void of() {
        MenuView.ButtonView buttonView1 = new MenuView.ButtonView();
        buttonView1.setId("");
        buttonView1.setName("");
        buttonView1.setOptions(new HashMap<>());
        Map<String, Object> options = new HashMap<>();
        MenuView.ButtonView buttonView = MenuView.ButtonView.of("test", "name", options);
        assertNotNull(buttonView.getId());
        assertNotNull(buttonView.getName());
        assertNotNull(buttonView.getOptions());
    }
}