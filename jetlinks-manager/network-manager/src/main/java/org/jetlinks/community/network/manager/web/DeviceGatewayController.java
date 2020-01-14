package org.jetlinks.community.network.manager.web;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.core.message.Message;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.service.DeviceGatewayService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("gateway/device")
@Resource(id = "device-gateway", name = "设备网关")
@Authorize
public class DeviceGatewayController implements ReactiveServiceCrudController<DeviceGatewayEntity, String> {

    private final DeviceGatewayService deviceGatewayService;

    @Override
    public DeviceGatewayService getService() {
        return deviceGatewayService;
    }

    private final DeviceGatewayManager gatewayManager;

    public DeviceGatewayController(DeviceGatewayManager gatewayManager, DeviceGatewayService deviceGatewayService) {
        this.gatewayManager = gatewayManager;
        this.deviceGatewayService = deviceGatewayService;
    }

    @PostMapping("/{id}/_startup")
    public Mono<Void> startup(@PathVariable String id) {
        return gatewayManager
            .getGateway(id)
            .flatMap(DeviceGateway::startup)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.enabled))
            .then();
    }

    @PostMapping("/{id}/_pause")
    public Mono<Void> pause(@PathVariable String id) {
        return gatewayManager
            .getGateway(id)
            .flatMap(DeviceGateway::pause)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.paused))
            .then();
    }

    @PostMapping("/{id}/_shutdown")
    public Mono<Void> shutdown(@PathVariable String id) {
        return gatewayManager
            .shutdown(id)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.disabled).then())
            ;
    }

    @GetMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Message> getMessages(@PathVariable String id) {
        return gatewayManager
            .getGateway(id)
            .flatMapMany(DeviceGateway::onMessage);
    }

    @GetMapping(value = "/providers")
    public Flux<DeviceGatewayProvider> getProviders() {
        return Flux.fromIterable(gatewayManager.getProviders());
    }

}
