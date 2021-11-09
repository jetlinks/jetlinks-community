package org.jetlinks.community.device.web;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.test.spring.TestJetLinksController;
import org.jetlinks.community.device.web.response.GatewayDeviceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@WebFluxTest(GatewayDeviceController.class)
class GatewayDeviceControllerTest extends TestJetLinksController {
    public static final String BASE_URL = "/device/gateway";
    public static final String PRODUCT_ID = "1236859833832701954";
    public static final String DeviceParentId = "1000";
    public static final String DeviceId = "1001";
    @Autowired
    private LocalDeviceInstanceService instanceService;

    @Autowired
    private LocalDeviceProductService productService;

    @Test
    void queryGatewayDevice() {

        PagerResult<?> responseBody = client.get()
            .uri(BASE_URL + "/_query")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(PagerResult.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody);
    }

    @Test
    void getGatewayInfo() {
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId(PRODUCT_ID);
        deviceProductEntity.setDeviceType(DeviceType.gateway);
        deviceProductEntity.setMessageProtocol("demo-v1");
        deviceProductEntity.setProjectName("TCPs");
        deviceProductEntity.setName("aaa");
        productService.save(deviceProductEntity).subscribe();
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(DeviceId);
        deviceInstanceEntity.setName("tcp1");
        deviceInstanceEntity.setProductName("aaa");
        //deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setParentId(DeviceParentId);
        deviceInstanceEntity.setProductId(PRODUCT_ID);
        DeviceInstanceEntity deviceInstanceEntityP = new DeviceInstanceEntity();
        deviceInstanceEntityP.setId(DeviceParentId);
        deviceInstanceEntityP.setName("tcp2");
        deviceInstanceEntityP.setProductName("aaa");
        //deviceInstanceEntityP.setState(DeviceState.online);
        deviceInstanceEntityP.setProductId(PRODUCT_ID);
        instanceService.save(deviceInstanceEntity).subscribe();
        instanceService.save(deviceInstanceEntityP).subscribe();
        GatewayDeviceInfo responseBody = client.get()
            .uri(BASE_URL + "/" + DeviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(GatewayDeviceInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody);
    }

    @Test
    void bindDevice() {
        GatewayDeviceInfo responseBody = client.post()
            .uri(BASE_URL +"/"+DeviceParentId +"/bind" + DeviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(GatewayDeviceInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody);
    }

    @Test
    void testBindDevice() {
        List<String> list = new ArrayList<>();
        list.add(DeviceId);
        GatewayDeviceInfo responseBody = client.post()
            .uri(BASE_URL +"/"+DeviceParentId +"/bind")
            .bodyValue(list)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(GatewayDeviceInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody);
    }

    @Test
    void unBindDevice() {
        GatewayDeviceInfo responseBody = client.post()
            .uri(BASE_URL +"/"+DeviceParentId +"/unbind" + DeviceId)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(GatewayDeviceInfo.class)
            .returnResult()
            .getResponseBody();
        assertNotNull(responseBody);
        System.out.println(responseBody);
    }
}