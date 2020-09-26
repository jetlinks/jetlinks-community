package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
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
@Tag(name = "设备网关管理")
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
    @SaveAction
    @Operation(summary = "启动网关")
    public Mono<Void> startup(@PathVariable
                              @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .getGateway(id)
            .flatMap(DeviceGateway::startup)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.enabled))
            .then();
    }

    @PostMapping("/{id}/_pause")
    @SaveAction
    @Operation(summary = "暂停")
    public Mono<Void> pause(@PathVariable
                            @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .getGateway(id)
            .flatMap(DeviceGateway::pause)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.paused))
            .then();
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .shutdown(id)
            .then(deviceGatewayService.updateState(id, NetworkConfigState.disabled).then())
            ;
    }

    @GetMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @QueryAction
    @Operation(summary = "从设备网关中订阅消息")
    public Flux<Message> getMessages(@PathVariable
                                     @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .getGateway(id)
            .flatMapMany(DeviceGateway::onMessage);
    }

    @GetMapping(value = "/providers")
    @Operation(summary = "获取支持的设备网关")
    public Flux<DeviceGatewayProvider> getProviders() {
        return Flux.fromIterable(gatewayManager.getProviders());
    }

}
