package org.jetlinks.community.auth.web;

import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.jetlinks.community.auth.entity.MenuButtonInfo;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(value = MenuController.class, properties = {
    "menu.allow-all-menus-users=test"
})
class MenuControllerTest extends TestJetLinksController {


    @Autowired
    private DefaultPermissionService permissionService;

    @Autowired
    private ReactiveUserService userService;

    @Test
    void testCrud() {
        MenuEntity sys = new MenuEntity();
        sys.setName("系统设置");
        sys.setUrl("/xxx");
        sys.setId("sys-crud");
        MenuEntity menu = new MenuEntity();
        menu.setId("menu-crud");
        menu.setName("菜单管理");
        menu.setUrl("/xxx");
        menu.setParentId(sys.getId());
        menu.setButtons(Arrays.asList(
            MenuButtonInfo.of("create", "创建菜单", "menu", "add"),
            MenuButtonInfo.of("delete", "删除菜单", "menu", "remove")
        ));

        sys.setChildren(Collections.singletonList(menu));

        client.patch()
            .uri("/menu")
            .bodyValue(sys)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.delete()
            .uri("/menu/{id}", sys.getId())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.get()
            .uri("/menu/_query/no-paging")
            .exchange()
            .expectBodyList(MenuEntity.class)
            .hasSize(0);
    }

    @Test
    void testGrant() {

        MenuEntity sys = new MenuEntity();
        sys.setName("系统设置");
        sys.setUrl("/xxx");
        sys.setId("sys");

        MenuEntity menu = new MenuEntity();
        menu.setId("menu");
        menu.setName("菜单管理");
        menu.setUrl("/xxx");
        menu.setParentId(sys.getId());
        menu.setButtons(Arrays.asList(
            MenuButtonInfo.of("create", "创建菜单", "menu", "add"),
            MenuButtonInfo.of("delete", "删除菜单", "menu", "remove")
        ));


        sys.setChildren(Collections.singletonList(menu));

        client.patch()
            .uri("/menu")
            .bodyValue(sys)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        MenuGrantRequest request = new MenuGrantRequest();
        request.setTargetType("user");
        request.setTargetId("test2");
        MenuView menuView = new MenuView();
        menuView.setId(menu.getId());
        menuView.setButtons(Arrays.asList(MenuView.ButtonView.of("create", "创建菜单", "", null)));
        menuView.setGranted(true);
        request.setMenus(Arrays.asList(menuView));

        client
            .put()
            .uri("/menu/user/test2/_grant")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        List<MenuView> tree = client
            .get()
            .uri("/menu/user/test2/_grant/tree")
            .exchange()
            .expectBodyList(MenuView.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(tree);
        assertFalse(tree.isEmpty());

        assertEquals(tree.get(0).getId(), sys.getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(menu.getId(), tree.get(0).getChildren().get(0).getId());

        assertEquals(2, tree.get(0).getChildren().get(0).getButtons().size());

        assertTrue(tree
            .get(0)
            .getChildren()
            .get(0)
            .getButton("create")
            .map(MenuView.ButtonView::isGranted)
            .orElse(false));
        assertFalse(tree
            .get(0)
            .getChildren()
            .get(0)
            .getButton("delete")
            .map(MenuView.ButtonView::isGranted)
            .orElse(true));

        client.delete()
            .uri("/menu/{id}", sys.getId())
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    void testAssetTypes() {
        //菜单
        MenuEntity sys = new MenuEntity();
        sys.setName("系统设置");
        sys.setUrl("/xxx");
        sys.setPermissions(Arrays.asList(PermissionInfo.of("test", new HashSet<>(Arrays.asList("save")))));
        sys.setId("asset-test");

        client.patch()
            .uri("/menu")
            .bodyValue(sys)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        //权限
        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setId("test");
        permissionEntity.setName("Test");
        permissionEntity.setActions(new ArrayList<>());

        permissionService
            .save(permissionEntity)
            .then()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

        //资产
        List<MenuController.AssetTypeView> view = client
            .post()
            .uri("/menu/asset-types")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("[{\"id\":\"asset-test\"}]")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(MenuController.AssetTypeView.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(view);
        assertEquals(1, view.size());

    }


    @Test
    void testPermissions() {
        //菜单
        MenuEntity sys = new MenuEntity();
        sys.setName("系统设置");
        sys.setUrl("/xxx");
        sys.setPermissions(Arrays.asList(PermissionInfo.of("test", new HashSet<>(Arrays.asList("save")))));
        sys.setId("asset-test");

        client.patch()
            .uri("/menu")
            .bodyValue(sys)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        //权限
        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setId("test");
        permissionEntity.setName("Test");
        permissionEntity.setActions(new ArrayList<>());

        permissionService
            .save(permissionEntity)
            .then()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();


        List<AuthorizationSettingDetail.PermissionInfo> view = client
            .post()
            .uri("/menu/permissions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("[{\"id\":\"asset-test\"}]")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(AuthorizationSettingDetail.PermissionInfo.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(view);
        assertEquals(1, view.size());
        assertEquals("test", view.get(0).getId());

    }

    @Override
    protected void initAuth(TestAuthentication authentication) {
        super.initAuth(authentication);
        authentication.addPermission("menu", "add");
    }
}