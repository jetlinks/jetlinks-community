package org.jetlinks.community.network.manager.service;

import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class DeviceGatewayConfigServiceTest {

    @Test
    void getProperties() {
        DeviceGatewayService gatewayService = Mockito.mock(DeviceGatewayService.class);


        DeviceGatewayConfigService service = new DeviceGatewayConfigService(gatewayService);

        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId("test");
        entity.setNetworkId("test");
        Mockito.when(gatewayService.findById(Mockito.anyString()))
            .thenReturn(Mono.just(entity));

        service.getProperties("test")
            .map(DeviceGatewayProperties::getNetworkId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }
}