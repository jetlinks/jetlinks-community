package org.jetlinks.community.device.web;


import com.google.common.cache.Cache;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(DeviceMessageController.class)
class DeviceMessageControllerTest extends TestJetLinksController {

    @Autowired
    private ClusterDeviceRegistry clusterDeviceRegistry;

    public static final String BASE_URL = "/device";
    public static final String DEVICE_ID = "1000";
    public static final String PRODUCT_ID = "1236859833832701954";
    @Test
    void getProperty() throws Exception {

//        registry.register()
        Class<? extends ClusterDeviceRegistry> aClass = clusterDeviceRegistry.getClass();
        Field operatorCache = aClass.getDeclaredField("operatorCache");
        System.out.println(operatorCache.getName());
        operatorCache.setAccessible(true);
        Cache<String, Mono<DeviceOperator>> cache = (Cache<String, Mono<DeviceOperator>>) operatorCache.get(clusterDeviceRegistry);
        System.out.println(cache.size());
        client.get()
            .uri(BASE_URL+"/"+DEVICE_ID+"/property/test")
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    void getStandardProperty() {
        client.get()
            .uri(BASE_URL+"/standard/"+DEVICE_ID+"/property/test")
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    void settingProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test","test");
        client.post()
            .uri(BASE_URL+"/setting/"+DEVICE_ID+"/property")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(properties)
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    void invokedFunction() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test","test");
        client.post()
            .uri(BASE_URL+"/invoked/"+DEVICE_ID+"/function/test")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(properties)
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }

    @Test
    void getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test","test");
        client.post()
            .uri(BASE_URL+"/"+DEVICE_ID+"/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(properties)
            .exchange()
            .expectStatus()
            .is4xxClientError();
    }
}