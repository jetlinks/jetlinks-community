package org.jetlinks.community.auth.web;

import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 机构管理.
 *
 * @author zhangji
 * @version 1.11 2021/9/28
 */
@DisplayName("机构管理：OrganizationController")
@WebFluxTest(OrganizationController.class)
class OrganizationControllerTest extends TestJetLinksController {
    public static final String BASE_URI = "/organization";


    @Override
    protected void initAuth(TestAuthentication authentication) {
        super.initAuth(authentication);
    }

    @Test
    void test() {
        // 创建
        for (OrganizationEntity organizationEntity : orgList()) {
            client.post()
                .uri(BASE_URI)
                .bodyValue(organizationEntity)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
        }

        // 查询全部-树结构
        List<OrganizationEntity> orgTreeList = client
            .get()
            .uri(BASE_URI + "/_all/tree")
            .exchange()
            .expectBodyList(OrganizationEntity.class)
            .returnResult()
            .getResponseBody();

        assertFalse(CollectionUtils.isEmpty(orgTreeList));
        OrganizationEntity top = orgTreeList.get(0);
        assertEquals("1", top.getId());

        List<OrganizationEntity> children = top.getChildren();
        assertFalse(CollectionUtils.isEmpty(children));
        assertEquals("2", children.get(0).getId());
        assertEquals("3", children.get(1).getId());
        assertFalse(CollectionUtils.isEmpty(children.get(0).getChildren()));
        assertEquals("4", children.get(0).getChildren().get(0).getId());

        // 查询全部-列表
        // 根据sortIndex字段排序
        List<OrganizationEntity> orgList = client
            .get()
            .uri(BASE_URI + "/_all")
            .exchange()
            .expectBodyList(OrganizationEntity.class)
            .returnResult()
            .getResponseBody();

        assertFalse(CollectionUtils.isEmpty(orgList));
        assertEquals("1", orgList.get(0).getId());
        assertEquals("2", orgList.get(1).getId());
        assertEquals("3", orgList.get(2).getId());
        assertEquals("4", orgList.get(3).getId());

        // 查询机构列表(包含子机构)树结构
        List<OrganizationEntity> orgIncludeChildrenTreeList = client
            .get()
            .uri(BASE_URI + "/_query/_children/tree?where=code = 1001&orgerBy=id")
            .exchange()
            .expectBodyList(OrganizationEntity.class)
            .returnResult()
            .getResponseBody();

        assertFalse(CollectionUtils.isEmpty(orgIncludeChildrenTreeList));
        assertEquals(2, orgIncludeChildrenTreeList.get(0).getChildren().size());

        // 查询机构列表(包含子机构)
        List<OrganizationEntity> orgIncludeChildrenList = client
            .get()
            .uri(BASE_URI + "/_query/_children?where=code = 1001&orgerBy=id")
            .exchange()
            .expectBodyList(OrganizationEntity.class)
            .returnResult()
            .getResponseBody();

        assertFalse(CollectionUtils.isEmpty(orgIncludeChildrenList));
        assertEquals(4, orgIncludeChildrenList.size());

        // 绑定用户到机构
        client.post()
            .uri(BASE_URI + "/{id}/users/_bind", "1")
            .body(Flux.just("test").collectList(), new ParameterizedTypeReference<List<String>>() {
            })
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        // 从机构解绑用户
        client.post()
            .uri(BASE_URI + "/{id}/users/_unbind", "1")
            .body(Flux.just("test").collectList(), new ParameterizedTypeReference<List<String>>() {
            })
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.delete()
            .uri(BASE_URI + "/{id}", "1")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

    }

    List<OrganizationEntity> orgList() {
        List<OrganizationEntity> orgList = new ArrayList<>();
        OrganizationEntity orgEntity = new OrganizationEntity();
        orgEntity.setId("1");
        orgEntity.setCode("1001");
        orgEntity.setName("总部");
        orgEntity.setType("默认");
        orgEntity.setDescribe("组织的最上级");
        Map<String, Object> properties = new HashMap<>();
        properties.put("province", "北京");
        properties.put("alias", "top");
        orgEntity.setProperties(properties);
        orgEntity.setParentId("");
        orgEntity.setPath("1001");
        orgEntity.setLevel(1);
        orgEntity.setSortIndex(1L);
        orgList.add(orgEntity);

        OrganizationEntity orgChildrenEntity1 = new OrganizationEntity();
        orgChildrenEntity1.setId("2");
        orgChildrenEntity1.setCode("1002");
        orgChildrenEntity1.setName("重庆分部");
        orgChildrenEntity1.setType("默认");
        Map<String, Object> childrenProperties1 = new HashMap<>();
        childrenProperties1.put("province", "重庆");
        childrenProperties1.put("alias", "child-1");
        orgChildrenEntity1.setProperties(childrenProperties1);
        orgChildrenEntity1.setParentId("1");
        orgChildrenEntity1.setPath("1001-1002");
        orgChildrenEntity1.setLevel(2);
        orgChildrenEntity1.setSortIndex(2L);
        orgList.add(orgChildrenEntity1);

        OrganizationEntity orgChildrenEntity2 = new OrganizationEntity();
        orgChildrenEntity2.setId("3");
        orgChildrenEntity2.setCode("1003");
        orgChildrenEntity2.setName("成都分部");
        orgChildrenEntity2.setType("默认");
        Map<String, Object> childrenProperties2 = new HashMap<>();
        childrenProperties2.put("province", "成都");
        childrenProperties2.put("alias", "child-2");
        orgChildrenEntity2.setProperties(childrenProperties2);
        orgChildrenEntity2.setParentId("1");
        orgChildrenEntity2.setPath("1001-1003");
        orgChildrenEntity2.setLevel(2);
        orgChildrenEntity2.setSortIndex(3L);
        orgList.add(orgChildrenEntity2);

        OrganizationEntity orgChildrenEntity3 = new OrganizationEntity();
        orgChildrenEntity3.setId("4");
        orgChildrenEntity3.setCode("1004");
        orgChildrenEntity3.setName("沙坪坝办事点");
        orgChildrenEntity3.setType("默认");
        Map<String, Object> childrenProperties3 = new HashMap<>();
        childrenProperties3.put("province", "重庆");
        childrenProperties3.put("alias", "child-3");
        orgChildrenEntity3.setProperties(childrenProperties3);
        orgChildrenEntity3.setParentId("2");
        orgChildrenEntity3.setPath("1001-1002-1004");
        orgChildrenEntity3.setLevel(3);
        orgChildrenEntity3.setSortIndex(4L);
        orgList.add(orgChildrenEntity3);

        return orgList;
    }
}
