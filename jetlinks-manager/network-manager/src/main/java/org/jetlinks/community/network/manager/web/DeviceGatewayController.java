package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.session.DeviceSessionInfo;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.DeviceGatewayService;
import org.jetlinks.community.network.manager.web.response.DeviceGatewayDetail;
import org.jetlinks.community.network.manager.web.response.DeviceGatewayProviderInfo;
import org.jetlinks.community.utils.ReactorUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;

@RestController
@RequestMapping("/gateway/device")
@Resource(id = "device-gateway", name = "设备接入网关")
@Authorize
@Tag(name = "设备接入管理")
@AllArgsConstructor
public class DeviceGatewayController implements ReactiveServiceCrudController<DeviceGatewayEntity, String> {

    private final DeviceGatewayService deviceGatewayService;
    private final ProtocolSupports protocolSupports;
    private final DeviceGatewayManager gatewayManager;
    private final DeviceSessionManager sessionManager;

    @Override
    @Generated
    public DeviceGatewayService getService() {
        return deviceGatewayService;
    }


    @PostMapping("/{id}/_startup")
    @SaveAction
    @Operation(summary = "启动网关")
    public Mono<Void> startup(@PathVariable
                              @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .start(id)
            .then(deviceGatewayService.updateState(id, DeviceGatewayState.enabled))
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
            .then(deviceGatewayService.updateState(id, DeviceGatewayState.paused))
            .then();
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网关ID") String id) {
        return gatewayManager
            .shutdown(id)
            .then(deviceGatewayService.updateState(id, DeviceGatewayState.disabled).then());
    }

    @GetMapping("/{id}/detail")
    @QueryAction
    @Operation(summary = "获取单个接入网关详情")
    public Mono<DeviceGatewayDetail> getDetail(@PathVariable
                                               @Parameter(description = "网关ID") String id) {
        return deviceGatewayService
            .findById(id)
            .flatMap(this::convertDetail);
    }

    @PostMapping("/detail/_query")
    @QueryAction
    @Operation(summary = "分页查询设备接入网关详情")
    public Mono<PagerResult<DeviceGatewayDetail>> queryGateway(@RequestBody Mono<QueryParamEntity> paramBody) {

        return paramBody
            .flatMap(param -> deviceGatewayService
                .queryPager(param)
                .flatMap(result -> Flux
                    .fromIterable(result.getData())
                    .index()
                    //转为详情
                    .flatMap(tp2 -> this.convertDetail(tp2.getT2()).map(detail -> Tuples.of(tp2.getT1(), detail)))
                    //重新排序,因为转为详情是异步的可能导致顺序乱掉
                    .sort(Comparator.comparingLong(Tuple2::getT1))
                    .map(Tuple2::getT2)
                    .collectList()
                    .map(detail -> PagerResult.of(result.getTotal(), detail, param)))
            );
    }

    private Mono<DeviceGatewayDetail> convertDetail(DeviceGatewayEntity entity) {
        DeviceGatewayDetail detail = DeviceGatewayDetail.of(entity);
        return Flux
            .merge(
                //通道信息
                gatewayManager
                    .getChannel(entity.getChannel(), entity.getChannelId())
                    .map(detail::with),
                //协议信息
                entity.getProtocol() == null ? Mono.empty() : protocolSupports
                    .getProtocol(entity.getProtocol())
                    .onErrorResume(err->Mono.empty())
                    .flatMap(detail::with)
            )
            .last(detail);
    }

    @GetMapping(value = "/providers")
    @Operation(summary = "获取支持的接入方式")
    public Flux<DeviceGatewayProviderInfo> getProviders() {
        return LocaleUtils
            .currentReactive()
            .flatMapMany(locale -> Flux
                .fromIterable(gatewayManager.getProviders())
                .map(provider -> DeviceGatewayProviderInfo.of(provider, locale)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取设备会话信息")
    public Flux<DeviceSessionInfo> sessions(@RequestParam(required = false) Long pageIndex,
                                            @RequestParam(required = false) Long pageSize) {
        return sessionManager
            .getSessionInfo()
            .as(ReactorUtils.limit(pageIndex, pageSize));
    }

    @GetMapping("/sessions/{serverId}")
    @Operation(summary = "获取设备会话信息")
    public Flux<DeviceSessionInfo> sessions(@PathVariable String serverId,
                                            @RequestParam(required = false) Long pageIndex,
                                            @RequestParam(required = false) Long pageSize) {
        return sessionManager
            .getSessionInfo(serverId)
            .as(ReactorUtils.limit(pageIndex, pageSize));
    }

    @DeleteMapping("/session/{deviceId}")
    @Operation(summary = "移除设备会话")
    @SaveAction
    public Mono<Long> removeSession(@PathVariable String deviceId) {
        return sessionManager.remove(deviceId,false);
    }


}
