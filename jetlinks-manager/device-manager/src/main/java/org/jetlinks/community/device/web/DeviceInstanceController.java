package org.jetlinks.community.device.web;

import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.excel.ESDevicePropertiesEntity;
import org.jetlinks.community.device.logger.DeviceOperationLog;
import org.jetlinks.community.device.response.*;
import org.jetlinks.community.device.service.DeviceOperationService;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/device-instance")
@Authorize
@Resource(id = "device-instance", name = "设备实例")
public class DeviceInstanceController implements
    ReactiveServiceCrudController<DeviceInstanceEntity, String> {

    @Autowired
    @Getter
    private LocalDeviceInstanceService service;

    @Autowired
    private DeviceOperationService operationService;

    @GetMapping("/all-info/{id:.+}")
    public Mono<DeviceAllInfoResponse> getDeviceAllInfo(@PathVariable String id) {
        return service.getDeviceAllInfo(id);
    }

    @GetMapping("/info/{id:.+}")
    public Mono<DeviceInfo> getDeviceInfoById(@PathVariable String id) {
        return service.getDeviceInfoById(id);
    }

    @GetMapping("/run-info/{id:.+}")
    public Mono<DeviceRunInfo> getRunDeviceInfoById(@PathVariable String id) {
        return service.getDeviceRunInfo(id);
    }

    @GetMapping("/{productId}/{deviceId}/properties")
    public Flux<ESDevicePropertiesEntity> getDeviceProperties(@PathVariable String productId, @PathVariable String deviceId) {
        return service.getProperties(productId, deviceId);
    }

    @GetMapping("/{deviceId}/property/{property:.+}")
    public Mono<ESDevicePropertiesEntity> getDeviceProperty(@PathVariable String deviceId, @PathVariable String property) {
        return service.getProperty(deviceId, property);
    }

    @PostMapping("/deploy/{deviceId:.+}")
    public Mono<DeviceDeployResult> deviceDeploy(@PathVariable String deviceId) {
        return service
            .deploy(deviceId);
    }

    @PostMapping("/cancelDeploy/{deviceId:.+}")
    public Mono<Integer> cancelDeploy(@PathVariable String deviceId) {
        return service.cancelDeploy(deviceId);
    }

    @PostMapping
    public Mono<DeviceInstanceEntity> add(@RequestBody Mono<DeviceInstanceEntity> payload) {
        return payload.flatMap(entity -> service
            .insert(Mono.just(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("设备ID已存在", err))
            .thenReturn(entity));
    }

    @GetMapping(value = "/deploy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaveAction
    public Flux<DeviceDeployResult> deployAll(QueryParamEntity query) {
        query.setPaging(false);
        return service
            .query(Mono.just(query))
            .as(service::deploy);
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
            .query(Mono.just(query.includes("id")))
            .map(DeviceInstanceEntity::getId)
            .buffer(50)
            .as(flux -> service.syncStateBatch(flux, true));
    }

    @GetMapping(value = "/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("批量导入数据")
    @SaveAction
    public Flux<ImportDeviceInstanceResult> doBatchImport(@RequestParam String fileUrl) {

        return service.doBatchImport(fileUrl);
    }


    @GetMapping("/operation/log")
    @QueryAction
    public Mono<PagerResult<DeviceOperationLog>> queryOperationLog(QueryParamEntity queryParam) {
        return operationService.queryPager(queryParam);
    }

    @PostMapping("/export")
    public Mono<Void> export(ServerHttpResponse response, QueryParam parameter) {
        return service.doExport(response, parameter, "设备实例.xlsx");
    }
}
