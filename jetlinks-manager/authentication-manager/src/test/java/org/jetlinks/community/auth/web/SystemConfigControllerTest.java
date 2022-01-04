package org.jetlinks.community.auth.web;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.auth.entity.SystemConfigEntity;
import org.jetlinks.community.test.spring.TestJetLinksController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(SystemConfigController.class)
class SystemConfigControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/system/config";

    @Autowired
    private  ReactiveRepository<SystemConfigEntity, String> repository;
    @Test
    void getFrontConfig() {
        SystemConfigEntity systemConfigEntity = new SystemConfigEntity();
        systemConfigEntity.setId("default");
        Map<String, Object> map = new HashMap<>();
        map.put("default","test");
        systemConfigEntity.setFrontConfig(map);
        assertNotNull(repository);
        repository.save(systemConfigEntity).subscribe();
        client.get()
            .uri(BASE_URL+"/front")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody()
            .jsonPath("$.default").isEqualTo("test");
    }

    @Test
    void saveFrontConfig() {

        Map<String, Object> map = new HashMap<>();
        map.put("name","chuan");
        assertNotNull(client);
        client.post()
            .uri(BASE_URL+"/front")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(map)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}