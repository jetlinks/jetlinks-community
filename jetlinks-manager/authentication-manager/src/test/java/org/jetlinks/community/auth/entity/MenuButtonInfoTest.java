package org.jetlinks.community.auth.entity;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class MenuButtonInfoTest {

    @Test
    void hasPermission() {
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        menuButtonInfo.setId("but_id");
        menuButtonInfo.setName("butt");
        List<PermissionInfo> permissions = new ArrayList<>();
        menuButtonInfo.setPermissions(permissions);
        Map<String, Object> options = new HashMap<>();
        menuButtonInfo.setOptions(options);

        assertNotNull(menuButtonInfo.getId());
        assertNotNull(menuButtonInfo.getName());
        assertNotNull(menuButtonInfo.getOptions());
        assertNotNull(menuButtonInfo.getPermissions());


        BiPredicate<String, Collection<String>> predicate = (permissionsId, actions)
            -> (!"".equals(permissionsId))||!actions.isEmpty();
        assertTrue(menuButtonInfo.hasPermission(predicate));

        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setPermission("");
        permissionInfo.setActions(new HashSet<>());
        permissions.add(permissionInfo);
        assertFalse(menuButtonInfo.hasPermission(predicate));
        permissionInfo.setPermission("test");
        permissionInfo.setActions(new HashSet<>());
        assertTrue(menuButtonInfo.hasPermission(predicate));
    }

    @Test
    void of() {
        MenuButtonInfo of = MenuButtonInfo.of("but_id", "button", "test", "aaa");
        assertNotNull(of);
    }
}