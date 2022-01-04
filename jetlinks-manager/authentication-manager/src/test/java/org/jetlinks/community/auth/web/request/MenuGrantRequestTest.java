package org.jetlinks.community.auth.web.request;

import org.jetlinks.community.auth.entity.MenuButtonInfo;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MenuGrantRequestTest {
    private static final String ID = "test";
    private static final String Buttons_ID = "button";
    @Test
    void toAuthorizationSettingDetail() {
        List<MenuView> menus = new ArrayList<>();
        MenuView menuView = new MenuView();
        menuView.setId("aa");
        menus.add(menuView);
        MenuView menuView1 = new MenuView();
        menuView1.setId(ID);
        //设置menuView1的buttons
        List<MenuView.ButtonView> buttons = new ArrayList<>();
        MenuView.ButtonView buttonView = new MenuView.ButtonView();
        buttonView.setId(Buttons_ID);
        buttons.add(buttonView);
        menuView1.setButtons(buttons);
        menus.add(menuView1);
        MenuGrantRequest request = new MenuGrantRequest("targetType","targetId",true,1,menus);
        assertNotNull(request);
        List<MenuEntity> menuEntities = new ArrayList<>();
        MenuEntity menuEntity = new MenuEntity();
        menuEntity.setId(ID);

        //设置menuEntity的permissions
        List<PermissionInfo> permissions = new ArrayList<>();
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setPermission("test");
        permissionInfo.setActions(new HashSet<>());
        menuEntity.setPermissions(permissions);

        //设置menuEntity的buttons
        List<MenuButtonInfo> buttons1 = new ArrayList<>();
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        menuButtonInfo.setId(Buttons_ID);
        //设置menuButtonInfo的permissions
        List<PermissionInfo> permissions1 = new ArrayList<>();
        PermissionInfo permissionInfo1 = new PermissionInfo();
        permissionInfo1.setPermission("t");
        permissionInfo1.setActions(new HashSet<>());
        permissions1.add(permissionInfo1);
        menuButtonInfo.setPermissions(permissions1);
        buttons1.add(menuButtonInfo);
        menuEntity.setButtons(buttons1);
        menuEntities.add(menuEntity);
        request.toAuthorizationSettingDetail(menuEntities);
        Set<String> set = new HashSet<>();
        set.add("aaa");
        permissionInfo1.setActions(set);
        request.toAuthorizationSettingDetail(menuEntities);

    }
}