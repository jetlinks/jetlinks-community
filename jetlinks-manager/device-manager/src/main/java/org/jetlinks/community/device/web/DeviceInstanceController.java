package org.jetlinks.community.device.web;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.excel.DeviceInstanceImportExportEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.io.excel.ImportExportService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final LocalDeviceProductService productService;

    private final ImportExportService importExportService;

    public DeviceInstanceController(LocalDeviceInstanceService service, TimeSeriesManager timeSeriesManager, DeviceRegistry registry, LocalDeviceProductService productService, ImportExportService importExportService) {
        this.service = service;
        this.timeSeriesManager = timeSeriesManager;
        this.registry = registry;
        this.productService = productService;
        this.importExportService = importExportService;
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
            .buffer(200)
            .publishOn(Schedulers.single())
            .concatMap(flux -> service.syncStateBatch(Flux.just(flux), true))
            .defaultIfEmpty(0);
    }

    //已废弃
    @GetMapping("/{productId:.+}/{deviceId:.+}/properties")
    @Deprecated
    @QueryAction
    public Flux<DevicePropertiesEntity> getDeviceLatestProperties(@PathVariable String productId,
                                                                  @PathVariable String deviceId) {
        return service.getDeviceLatestProperties(deviceId);
    }

    @GetMapping("/{deviceId:.+}/properties/latest")
    @QueryAction
    public Flux<DevicePropertiesEntity> getDeviceLatestProperties(@PathVariable String deviceId) {
        return service.getDeviceLatestProperties(deviceId);
    }

    @GetMapping("/{deviceId:.+}/property/{property:.+}")
    @QueryAction
    public Mono<DevicePropertiesEntity> getDeviceLatestProperty(@PathVariable String deviceId, @PathVariable String property) {
        return service.getDeviceLatestProperty(deviceId, property);
    }

    @GetMapping("/{deviceId:.+}/event/{eventId}")
    @QueryAction
    public Mono<PagerResult<Map<String, Object>>> queryPagerByDeviceEvent(QueryParamEntity queryParam,
                                                                          @RequestParam(defaultValue = "false") boolean format,
                                                                          @PathVariable String deviceId,
                                                                          @PathVariable String eventId) {
        return service.queryDeviceEvent(deviceId, eventId, queryParam, format);
    }

    @GetMapping("/{deviceId:.+}/properties/_query")
    @QueryAction
    public Mono<PagerResult<DevicePropertiesEntity>> queryDeviceProperties(@PathVariable String deviceId, QueryParamEntity entity) {
        return service.queryDeviceProperties(deviceId, entity);
    }

    @GetMapping("/{deviceId:.+}/logs")
    @QueryAction
    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(@PathVariable String deviceId,
                                                                      QueryParamEntity entity) {
        return service.queryDeviceLog(deviceId, entity);
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
            .flatMapMany(auth -> productService
                .createQuery()
                .fetch()
                .collectList()
                .flatMapMany(productEntities -> {
                    Map<String, String> productNameMap = productEntities.stream()
                        .collect(Collectors.toMap(DeviceProductEntity::getName, DeviceProductEntity::getId, (_1, _2) -> _1));
                    return importExportService
                        .doImport(DeviceInstanceImportExportEntity.class, fileUrl)
                        .map(result -> {
                            try {
                                DeviceInstanceImportExportEntity importExportEntity = result.getResult();
                                DeviceInstanceEntity entity = FastBeanCopier.copy(importExportEntity, new DeviceInstanceEntity());
                                String productId = productNameMap.get(importExportEntity.getProductName());
                                if (StringUtils.isEmpty(productId)) {
                                    throw new BusinessException("设备型号不存在");
                                }
                                if (StringUtils.isEmpty(entity.getId())) {
                                    throw new BusinessException("设备ID不能为空");
                                }

                                entity.setProductId(productId);
                                entity.setState(DeviceState.notActive);
                                return entity;
                            } catch (Throwable e) {
                                throw new BusinessException("第" +
                                    (result.getRowIndex() + 2)
                                    + "行:" + e.getMessage());
                            }
                        });
                })
                .buffer(20)
                .publishOn(Schedulers.single())
                .concatMap(list -> service.save(Flux.fromIterable(list)))
                .map(ImportDeviceInstanceResult::success))
            .onErrorResume(err -> Mono.just(ImportDeviceInstanceResult.error(err)))
            ;
    }

    DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @PostMapping("/export")
    @QueryAction
    @SneakyThrows
    public Mono<Void> export(ServerHttpResponse response, QueryParam parameter) {
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=".concat(URLEncoder.encode("设备实例.xlsx", StandardCharsets.UTF_8.displayName())));
        return Authentication
            .currentReactive()
            .flatMap(auth -> {
                parameter.setPaging(false);
                return response.writeWith(Mono.create(sink -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ExcelWriter excelWriter = EasyExcel.write(out, DeviceInstanceImportExportEntity.class).build();
                    WriteSheet writeSheet = EasyExcel.writerSheet().build();
                    service.query(parameter)
                        .map(entity -> FastBeanCopier.copy(entity, new DeviceInstanceImportExportEntity()))
                        .buffer(100)
                        .doOnNext(list -> excelWriter.write(list, writeSheet))
                        .doFinally(s -> {
                            excelWriter.finish();
                            sink.success(bufferFactory.wrap(out.toByteArray()));
                        })
                        .doOnError(sink::error)
                        .subscribe();
                }));
            });
    }
}
