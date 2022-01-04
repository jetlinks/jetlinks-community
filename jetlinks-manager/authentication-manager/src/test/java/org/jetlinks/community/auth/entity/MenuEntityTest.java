package org.jetlinks.community.auth.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class MenuEntityTest {
    private static final String ID = "test";

    @Test
    void copy() {
        MenuEntity menuEntity = new MenuEntity();

        //设置menuEntity的buttons
        List<MenuButtonInfo> buttons = new ArrayList<>();
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        menuButtonInfo.setId(ID);
        buttons.add(menuButtonInfo);
        menuEntity.setButtons(buttons);

        Predicate<MenuButtonInfo> buttonPredicate = buttonInfo -> buttonInfo.getId() != null && !"".equals(buttonInfo.getId());
        MenuEntity copy = menuEntity.copy(buttonPredicate);
        assertNotNull(copy);
    }

    @Test
    void hasPermission() {
        MenuEntity menuEntity = new MenuEntity();

        BiPredicate<String, Collection<String>> predicate =
            (s, list) -> s != null && !"".equals(s)||!list.isEmpty();
        assertFalse(menuEntity.hasPermission(predicate));
        List<MenuButtonInfo> buttons = new ArrayList<>();
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        buttons.add(menuButtonInfo);
        menuEntity.setButtons(buttons);
        assertTrue(menuEntity.hasPermission(predicate));
        List<PermissionInfo> permissions = new ArrayList<>();
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setPermission("");
        permissionInfo.setActions(new HashSet<>());
        permissions.add(permissionInfo);
        menuButtonInfo.setPermissions(permissions);
        assertFalse(menuEntity.hasPermission(predicate));
    }

    @Test
    void getButton() {
        MenuEntity menuEntity = new MenuEntity();
        menuEntity.getButton(ID);

        List<MenuButtonInfo> buttons = new ArrayList<>();
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        menuButtonInfo.setId(ID);
        buttons.add(menuButtonInfo);
        menuEntity.setButtons(buttons);
        MenuButtonInfo menuButtonInfo1 = menuEntity.getButton(ID).orElse(new MenuButtonInfo());
        assertNotNull(menuButtonInfo1);

    }
}