package org.jetlinks.community.auth.web;

import org.jetlinks.community.auth.entity.MenuButtonInfo;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.jetlinks.community.auth.service.AuthorizationSettingDetailService;
import org.jetlinks.community.auth.service.DefaultMenuService;
import org.jetlinks.community.auth.web.request.MenuGrantRequest;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest(MenuController.class)
class MenuControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/menu";

    @Autowired
    private DefaultMenuService defaultMenuService;

    @Test
    void getService() {
        new MenuController(defaultMenuService, Mockito.mock(AuthorizationSettingDetailService.class)).getService();
    }


    @Test
    @Order(1)
    void getUserMenuAsTree() {
        MenuEntity menuEntity = new MenuEntity();
        menuEntity.setStatus((byte) 1);
        menuEntity.setId("test");
        menuEntity.setName("test");
        List<PermissionInfo> permissions=new ArrayList<>();
        PermissionInfo permissionInfo = new PermissionInfo();
        Set<String> set = new HashSet<>();
        set.add("test");
        permissionInfo.setActions(set);
        permissionInfo.setPermission("test");
        permissions.add(permissionInfo);
        menuEntity.setPermissions(permissions);
        MenuEntity parent = new MenuEntity();
        parent.setStatus((byte) 1);
        parent.setId("parent");
        parent.setName("parent");
        List<PermissionInfo> permissions1=new ArrayList<>();
        PermissionInfo permissionInfo1 = new PermissionInfo();
        Set<String> set1 = new HashSet<>();
        set1.add("test1");
        permissionInfo1.setActions(set1);
        permissionInfo1.setPermission("test1");
        permissions1.add(permissionInfo1);
        parent.setPermissions(permissions1);
        menuEntity.setParentId("parent");
        defaultMenuService.save(parent).subscribe();
        defaultMenuService.save(menuEntity).subscribe();
        List<MenuView> responseBody = client.get()
            .uri(BASE_URL + "/user-own/tree")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(MenuView.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("parent",responseBody.get(0).getName());
        assertEquals("test",responseBody.get(0).getChildren().get(0).getName());

    }


    @Test
    @Order(4)
    void getUserMenuAsList(){

        MenuEntity menuEntity = new MenuEntity();
        menuEntity.setStatus((byte) 1);
        menuEntity.setId("test");
        menuEntity.setName("test");
        List<PermissionInfo> permissions=new ArrayList<>();
        PermissionInfo permissionInfo = new PermissionInfo();
        Set<String> set = new HashSet<>();
        set.add("test");
        permissionInfo.setActions(set);
        permissionInfo.setPermission("test");
        permissions.add(permissionInfo);
        menuEntity.setPermissions(permissions);
        List<MenuButtonInfo> buttons =new ArrayList<>();
        MenuButtonInfo menuButtonInfo = new MenuButtonInfo();
        menuButtonInfo.setPermissions(permissions);
        buttons.add(menuButtonInfo);
        menuEntity.setButtons(buttons);
        MenuEntity parent = new MenuEntity();
        parent.setStatus((byte) 1);
        parent.setId("parent");
        parent.setName("parent");
        List<PermissionInfo> permissions1=new ArrayList<>();
        PermissionInfo permissionInfo1 = new PermissionInfo();
        Set<String> set1 = new HashSet<>();
        set1.add("test1");
        permissionInfo1.setActions(set1);
        permissionInfo1.setPermission("test1");
        permissions1.add(permissionInfo1);
        parent.setPermissions(permissions1);
        List<MenuButtonInfo> buttons1 =new ArrayList<>();
        MenuButtonInfo menuButtonInfo1 = new MenuButtonInfo();
        menuButtonInfo1.setPermissions(permissions1);
        buttons.add(menuButtonInfo1);
        parent.setButtons(buttons1);
        //设置父菜单
        menuEntity.setParentId("parent");
        defaultMenuService.save(parent).subscribe();
        defaultMenuService.save(menuEntity).subscribe();

        client.get()
            .uri(BASE_URL+"/user-own/list")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(2)
    void grant() {
        MenuEntity menuEntity = new MenuEntity();
        menuEntity.setStatus((byte) 1);
        menuEntity.setId("test");
        menuEntity.setName("test");
        List<PermissionInfo> permissions=new ArrayList<>();
        PermissionInfo permissionInfo = new PermissionInfo();
        Set<String> set = new HashSet<>();
        set.add("test");
        permissionInfo.setActions(set);
        permissionInfo.setPermission("test");
        permissions.add(permissionInfo);
        menuEntity.setPermissions(permissions);
        defaultMenuService.save(menuEntity).subscribe();
        MenuGrantRequest menuGrantRequest = new MenuGrantRequest();
        menuGrantRequest.setTargetId("test");
        menuGrantRequest.setTargetType("test");
        List<MenuView> menus = new ArrayList<>();
        MenuView menuView = new MenuView();
        menuView.setId("test");
        menuView.setName("test");
        menus.add(menuView);
        menuGrantRequest.setMenus(menus);
        client.put()
            .uri(BASE_URL+"/_grant")
            .bodyValue(menuGrantRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void getGrantInfoTree() {
        List<MenuView> responseBody = client.get()
            .uri(BASE_URL + "/test/test/_grant/list")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(MenuView.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("parent",responseBody.get(0).getName());

    }

    @Test
    @Order(5)
    void getGrantInfo() {

        List<MenuView> responseBody = client.get()
            .uri(BASE_URL + "/test/test/_grant/tree")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(MenuView.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals("parent",responseBody.get(0).getName());
    }
}