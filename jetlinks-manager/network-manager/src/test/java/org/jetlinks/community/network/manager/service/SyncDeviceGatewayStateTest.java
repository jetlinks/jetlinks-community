package org.jetlinks.community.network.manager.service;

import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class SyncDeviceGatewayStateTest {
    private final static String ID = "test";
    @Test
    void run() {
        DeviceGatewayService gatewayService = Mockito.mock(DeviceGatewayService.class);
        DeviceGatewayManager gatewayManager = Mockito.mock(DeviceGatewayManager.class);
        ReactiveQuery<DeviceGatewayEntity> query = Mockito.mock(ReactiveQuery.class);
        SyncDeviceGatewayState service = new SyncDeviceGatewayState(gatewayService,gatewayManager);
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId(ID);
        Mockito.when(gatewayService.createQuery())
            .thenReturn(query);
        Mockito.when(query.where())
            .thenReturn(query);
        Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.just(entity));

        DeviceGateway gateway = Mockito.mock(DeviceGateway.class);

        Mockito.when(gatewayManager.getGateway(Mockito.anyString()))
            .thenReturn(Mono.just(gateway));
        Mockito.when(gateway.startup())
            .thenReturn(Mono.just(1).then());
        service.run();

    }
    @Test
    void test() throws Exception {
        DeviceGatewayService gatewayService = Mockito.mock(DeviceGatewayService.class);
        DeviceGatewayManager gatewayManager = Mockito.mock(DeviceGatewayManager.class);
        ReactiveQuery<DeviceGatewayEntity> query = Mockito.mock(ReactiveQuery.class);
        SyncDeviceGatewayState service = new SyncDeviceGatewayState(gatewayService,gatewayManager);
        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId(ID);
        Mockito.when(gatewayService.createQuery())
            .thenReturn(query);
        Mockito.when(query.where())
            .thenReturn(query);
        Mockito.when(query.and(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(query);
        Mockito.when(query.fetch())
            .thenReturn(Flux.error(new NotFoundException()));
        service.run();
        Thread.sleep(7000);
    }
}