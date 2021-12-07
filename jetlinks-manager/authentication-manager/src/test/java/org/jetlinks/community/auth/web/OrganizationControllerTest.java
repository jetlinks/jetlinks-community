package org.jetlinks.community.auth.web;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.system.authorization.api.entity.DimensionEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebFluxTest(OrganizationController.class)
class OrganizationControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/organization";

    //    @Autowired
//    private DefaultDimensionService dimensionService;
//
    @Test
    @Order(1)
    void addOrg() {
        String s = "[\n" +
            "  {\n" +
            "    \"id\": \"test\",\n" +
            "    \"parentId\": \"\",\n" +
            "    \"path\": \"\",\n" +
            "    \"sortIndex\": 1,\n" +
            "    \"level\": 1,\n" +
            "    \"typeId\": \"org\",\n" +
            "    \"name\": \"test\",\n" +
            "    \"describe\": \"\",\n" +
            "    \"properties\": {},\n" +
            "    \"children\": [\n" +
            "      {\n" +
            "        \"id\": \"test1\",\n" +
            "        \"parentId\": \"test\",\n" +
            "        \"path\": \"\",\n" +
            "        \"sortIndex\": 2,\n" +
            "        \"level\": 2,\n" +
            "        \"typeId\": \"org\",\n" +
            "        \"name\": \"test1\",\n" +
            "        \"describe\": \"\",\n" +
            "        \"properties\": {},\n" +
            "        \"children\": []\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]";
        client.post()
            .uri(BASE_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }


    @Test
    @Order(2)
    void getAllOrgTree() {
        List<DimensionEntity> responseBody = client.get()
            .uri(BASE_URL + "/_all/tree")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DimensionEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        //assertEquals("test",responseBody.get(0).getName());
    }

    @Test
    @Order(2)
    void getAllOrg() {
        List<DimensionEntity> responseBody = client.get()
            .uri(BASE_URL + "/_all")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DimensionEntity.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        //assertEquals("test",responseBody.get(0).getName());
    }

    @Test
    @Order(2)
    void queryDimension() {
        PagerResult<?> responseBody = client.get()
            .uri(BASE_URL + "/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(PagerResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(2,responseBody.getTotal());

    }

    @Test
    @Order(2)
    void queryChildrenTree() {
        List<?> responseBody = client.get()
            .uri(BASE_URL + "/_query/_children/tree")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(List.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
        assertNotNull(responseBody);
        assertEquals(1,responseBody.size());
    }

    @Test
    @Order(2)
    void queryChildren() {
        List<DimensionEntity> responseBody = client.get()
            .uri(BASE_URL + "/_query/_children")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(DimensionEntity.class)
            .returnResult()
            .getResponseBody();
        System.out.println(responseBody);
        assertNotNull(responseBody);
        assertEquals(3,responseBody.size());
        assertEquals("test",responseBody.get(0).getName());
    }


    @Test
    @Order(3)
    void updateOrg() {
        DimensionEntity dimensionEntity = new DimensionEntity();
        dimensionEntity.setId("test");
        dimensionEntity.setName("ccc");
        client.put()
            .uri(BASE_URL+"/test")
            .bodyValue(dimensionEntity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(3)
    void saveOrg() {
        String s = "[\n" +
            "  {\n" +
            "    \"id\": \"xxx\",\n" +
            "    \"parentId\": \"\",\n" +
            "    \"path\": \"\",\n" +
            "    \"sortIndex\": 1,\n" +
            "    \"level\": 1,\n" +
            "    \"typeId\": \"org\",\n" +
            "    \"name\": \"test\",\n" +
            "    \"describe\": \"\",\n" +
            "    \"properties\": {},\n" +
            "    \"children\": [\n" +
            "      {\n" +
            "        \"id\": \"test2\",\n" +
            "        \"parentId\": \"xxx\",\n" +
            "        \"path\": \"\",\n" +
            "        \"sortIndex\": 2,\n" +
            "        \"level\": 2,\n" +
            "        \"typeId\": \"org\",\n" +
            "        \"name\": \"test1\",\n" +
            "        \"describe\": \"\",\n" +
            "        \"properties\": {},\n" +
            "        \"children\": []\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]";
        client.patch()
            .uri(BASE_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(s)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(4)
    void deleteOrg() {
        client.delete()
            .uri(BASE_URL+"/xxx")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @Order(4)
    void bindUser() {
        List<String> list = new ArrayList<>();
        list.add("user1");
        Integer responseBody = client.post()
            .uri(BASE_URL + "/test/users/_bind")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1,responseBody);
    }

    @Test
    @Order(5)
    void unbindUser() {
        List<String> list = new ArrayList<>();
        list.add("user1");
        Integer responseBody = client.post()
            .uri(BASE_URL + "/test/users/_unbind")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1,responseBody);
    }
}