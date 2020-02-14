package org.jetlinks.community.device.web;

import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceOperationLogEntity;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.response.*;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping({"/device-instance", "/device/instance"})
@Authorize
@Resource(id = "device-instance", name = "设备实例")
@Slf4j
public class DeviceInstanceController implements
    ReactiveServiceCrudController<DeviceInstanceEntity, String> {

    @Getter
    private final LocalDeviceInstanceService service;

    private final TimeSeriesManager timeSeriesManager;

    private final DeviceRegistry registry;

    public DeviceInstanceController(LocalDeviceInstanceService service, TimeSeriesManager timeSeriesManager, DeviceRegistry registry) {
        this.service = service;
        this.timeSeriesManager = timeSeriesManager;
        this.registry = registry;
    }

    @GetMapping({
        "/all-info/{id:.+}", //todo 即将删除
        "/{id:.+}/detail"
    })
    @QueryAction
    public Mono<DeviceAllInfoResponse> getDeviceAllInfo(@PathVariable String id) {
        return service.getDeviceAllInfo(id);
    }

    @GetMapping("/info/{id:.+}")
    @QueryAction
    public Mono<DeviceInfo> getDeviceInfoById(@PathVariable String id) {
        return service.getDeviceInfoById(id);
    }

    @GetMapping("/run-info/{id:.+}")
    @QueryAction
    public Mono<DeviceRunInfo> getRunDeviceInfoById(@PathVariable String id) {
        return service.getDeviceRunInfo(id);
    }

    @PostMapping({
        "/deploy/{deviceId:.+}",//todo 即将移除
        "/{deviceId:.+}/deploy"
    })
    @SaveAction
    public Mono<DeviceDeployResult> deviceDeploy(@PathVariable String deviceId) {
        return service.deploy(deviceId);
    }

    @PostMapping({
        "/cancelDeploy/{deviceId:.+}", //todo 即将移除
        "/{deviceId:.+}/undeploy"
    })
    @SaveAction
    public Mono<Integer> cancelDeploy(@PathVariable String deviceId) {
        return service.cancelDeploy(deviceId);
    }


    @PostMapping("/{deviceId:.+}/disconnect")
    @SaveAction
    public Mono<Boolean> disconnect(@PathVariable String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMapMany(DeviceOperator::disconnect)
            .singleOrEmpty();
    }

    @PostMapping
    public Mono<DeviceInstanceEntity> add(@RequestBody Mono<DeviceInstanceEntity> payload) {
        return payload.flatMap(entity -> service.insert(Mono.just(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("设备ID已存在", err))
            .thenReturn(entity));
    }

    @GetMapping(value = "/deploy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    public Flux<DeviceDeployResult> deployAll(QueryParamEntity query) {
        query.setPaging(false);
        return service.query(query).as(service::deploy);
    }

    /**
     * 同步设备真实状态
     *
     * @param query 过滤条件
     * @return 实时同步结果
     */
    @GetMapping(value = "/state/_sync", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    public Flux<Integer> syncDeviceState(QueryParamEntity query) {
        query.setPaging(false);
        return service
            .query(query.includes("id"))
            .map(DeviceInstanceEntity::getId)
            .buffer(50)
            .as(flux -> service.syncStateBatch(flux, true));
    }

    @GetMapping("/{productId:.+}/{deviceId:.+}/properties")
    @QueryAction
    public Flux<DevicePropertiesEntity> getDeviceProperties(@PathVariable String productId, @PathVariable String deviceId) {
        return service.getProperties(productId, deviceId);
    }

    @GetMapping("/{deviceId:.+}/property/{property:.+}")
    @QueryAction
    public Mono<DevicePropertiesEntity> getDeviceProperty(@PathVariable String deviceId, @PathVariable String property) {
        return service.getProperty(deviceId, property);
    }

    @GetMapping({
        "/{deviceId:.+}/logs",
    })
    @QueryAction
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable String deviceId,
                                                                      QueryParamEntity entity) {
        return registry.getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> timeSeriesManager
                .getService(DeviceTimeSeriesMetric.deviceLogMetric(productId))
                .queryPager(entity.and("deviceId", TermType.eq, deviceId),
                    data -> data.as(DeviceOperationLogEntity.class)))
            .defaultIfEmpty(PagerResult.empty());
    }

    //已废弃
    @GetMapping("/operation/log")
    @QueryAction
    @Deprecated
    public Mono<PagerResult<DeviceOperationLogEntity>> queryOperationLog(QueryParamEntity queryParam) {
        return timeSeriesManager.getService(TimeSeriesMetric.of("device_operation")).queryPager(queryParam, data -> data.as(DeviceOperationLogEntity.class));
    }

    @GetMapping(value = "/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("批量导入数据")
    @SaveAction
    public Flux<ImportDeviceInstanceResult> doBatchImport(@RequestParam String fileUrl) {

        return Authentication
            .currentReactive()
            .flatMapMany(auth -> service.doBatchImport(fileUrl));
    }

    @PostMapping("/export")
    public Mono<Void> export(ServerHttpResponse response, QueryParam parameter) throws IOException {
        return Authentication
            .currentReactive()
            .flatMap(auth -> {
                return Mono.fromCallable(() -> service.doExport(response, parameter, "设备实例.xlsx"));
            }).then();
    }
}
