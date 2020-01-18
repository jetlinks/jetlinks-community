package org.jetlinks.community.device.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.device.response.*;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.excel.DeviceInstanceImportExportEntity;
import org.jetlinks.community.device.entity.excel.ESDevicePropertiesEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.events.handler.DeviceEventIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备实例服务
 *
 * @author zhouhao
 * @author bestfeng
 *
 * TODO 一团乱，等待重构.
 */
@Service
@Slf4j
public class LocalDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private LocalDeviceProductService deviceProductService;

    @Autowired
    private ImportExportService importExportService;

    @Autowired
    private MessageGateway messageGateway;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private LogService logService;

    public Mono<DeviceAllInfoResponse> getDeviceAllInfo(String id) {
        return findById(Mono.justOrEmpty(id))
            .zipWhen(instance -> deviceProductService.findById(Mono.justOrEmpty(instance.getProductId())), DeviceInfo::of)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .zipWhen(deviceInfo -> getDeviceRunRealInfo(id), DeviceAllInfoResponse::of)
            .zipWhen(info -> getProperties(info.getDeviceInfo().getProductId(), id)
                    .collect(Collectors.toMap(ESDevicePropertiesEntity::getProperty, ESDevicePropertiesEntity::getFormatValue)),
                DeviceAllInfoResponse::ofProperties)
            .zipWhen(info -> {
                    DeviceMetadata deviceMetadata = new JetLinksDeviceMetadata(JSON.parseObject(info.getDeviceInfo().getDeriveMetadata()));
                    return getEventCounts(deviceMetadata.getEvents(), id, info.getDeviceInfo().getProductId());
                },
                DeviceAllInfoResponse::ofEventCounts);
    }

    private Mono<DeviceRunInfo> getDeviceRunRealInfo(String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(deviceOperator -> Mono.zip(
                deviceOperator.getOnlineTime().switchIfEmpty(Mono.just(0L)),// 1
                deviceOperator.getOfflineTime().switchIfEmpty(Mono.just(0L)),// 2
                deviceOperator.checkState()
                    .switchIfEmpty(deviceOperator.getState())
                    .map(DeviceState::of)
                    .defaultIfEmpty(DeviceState.notActive),// 3
                deviceOperator.getConfig(DeviceConfigKey.metadata).switchIfEmpty(Mono.just(""))//4
                ).map(tuple4 -> DeviceRunInfo.of(
                tuple4.getT1(), //1. 上线时间
                tuple4.getT2(), //2. 离线时间
                tuple4.getT3(), //3. 状态
                tuple4.getT4()  //4. 设备模型元数据
                )
                ).flatMap(deviceRunInfo -> createUpdate()
                    .set(DeviceInstanceEntity::getState, deviceRunInfo.getState())
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .thenReturn(deviceRunInfo))
            );
    }

    /**
     * 获取设备事件上报次数
     *
     * @param events    设备事件元数据
     * @param deviceId  设备Id
     * @param productId 型号id
     * @return
     */
    private Mono<Map<String, Object>> getEventCounts(List<EventMetadata> events, String deviceId, String productId) {
        return Flux.merge(events
            .stream()
            .map(Metadata::getId)
            .map(eventId -> {
                    QueryParam queryParam = Query.of().where(DeviceInstanceEntity::getId, deviceId).getParam();
                    return logService.queryPagerByDeviceEvent(queryParam, productId, eventId)
                        .map(PagerResult::getTotal)
                        .map(count -> new EventCount(eventId, count));
                }
            )
            .collect(Collectors.toList()))
            .collectList()
            .map(list -> list.stream().collect(Collectors.toMap(EventCount::getEventId, EventCount::getCount)))
            ;
    }


    /**
     * 发布设备到设备注册中心
     *
     * @param id 设备ID
     * @return
     */
    public Mono<DeviceDeployResult> deploy(String id) {
        return findById(Mono.just(id))
            .flux()
            .as(this::deploy)
            .next();
    }

    /**
     * 批量发布设备到设备注册中心
     *
     * @param flux 设备实例流
     * @return 发布数量
     */
    public Flux<DeviceDeployResult> deploy(Flux<DeviceInstanceEntity> flux) {
        return flux
            .flatMap(instance ->
                registry.registry(org.jetlinks.core.device.DeviceInfo.builder()
                    .id(instance.getId())
                    .productId(instance.getProductId())
                    .build())
                    //设置其他配置信息
                    .flatMap(deviceOperator -> deviceOperator.getState()
                        .flatMap(r -> {
                            if (r.equals(org.jetlinks.core.device.DeviceState.unknown) ||
                                r.equals(org.jetlinks.core.device.DeviceState.noActive)) {
                                instance.setState(DeviceState.offline);
                                return deviceOperator.putState(org.jetlinks.core.device.DeviceState.offline);
                            }
                            instance.setState(DeviceState.of(r));
                            return Mono.just(true);
                        })
                        .flatMap(success -> success ? Mono.just(deviceOperator) : Mono.empty())
                    )
                    .thenReturn(instance))
            .buffer(50)
            .publishOn(Schedulers.single())
            .flatMap(all -> Flux.fromIterable(all)
                .groupBy(DeviceInstanceEntity::getState)
                .flatMap(group ->
                    group.map(DeviceInstanceEntity::getId)
                        .collectList()
                        .flatMap(list -> createUpdate()
                            .where()
                            .set(DeviceInstanceEntity::getState, group.key())
                            .set(DeviceInstanceEntity::getRegistryTime, new Date())
                            .in(DeviceInstanceEntity::getId, list)
                            .execute()
                            .map(r -> DeviceDeployResult.success(list.size()))
                            .onErrorResume(err -> Mono.just(DeviceDeployResult.error(err.getMessage()))))));
    }

    public Mono<Integer> cancelDeploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry
                .unRegistry(id)
                .then(createUpdate()
                    .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                    .where(DeviceInstanceEntity::getId, id)
                    .execute()));
    }

    public Mono<DeviceRunInfo> getDeviceRunInfo(String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(deviceOperator -> Mono.zip(
                deviceOperator.getOnlineTime().switchIfEmpty(Mono.just(0L)),// 1
                deviceOperator.getOfflineTime().switchIfEmpty(Mono.just(0L)),// 2
                deviceOperator.checkState()
                    .switchIfEmpty(deviceOperator.getState())
                    .map(DeviceState::of)
                    .defaultIfEmpty(DeviceState.notActive),// 3
                deviceOperator.getConfig(DeviceConfigKey.metadata).switchIfEmpty(Mono.just(""))//4
                ).map(tuple4 -> DeviceRunInfo.of(
                tuple4.getT1(), //1. 上线时间
                tuple4.getT2(), //2. 离线时间
                tuple4.getT3(), //3. 状态
                tuple4.getT4()  //4. 设备模型元数据
                )
                ).flatMap(deviceRunInfo -> createUpdate()
                    .set(DeviceInstanceEntity::getState, deviceRunInfo.getState())
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .thenReturn(deviceRunInfo))
            );
    }


    @PostConstruct
    public void init() {

        //订阅设备上下线
        FluxUtils.bufferRate(messageGateway
            .subscribe("/device/*/online", "/device/*/offline")
            .flatMap(message -> Mono.fromCallable(() -> {

                if (message.getMessage() instanceof EncodableMessage) {
                    Object msg = ((EncodableMessage) message.getMessage()).getNativePayload();
                    if (msg instanceof DeviceOnlineMessage) {
                        return ((DeviceOnlineMessage) msg).getDeviceId();
                    }
                    if (msg instanceof DeviceOfflineMessage) {
                        return ((DeviceOfflineMessage) msg).getDeviceId();
                    }
                }
                return null;
            })), 800, 200, Duration.ofSeconds(2))
            .flatMap(list -> syncStateBatch(Flux.just(list), false).count())
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .subscribe((i) -> log.info("同步设备状态成功:{}", i));

    }

    public Mono<DeviceInfo> getDeviceInfoById(String id) {
        return findById(Mono.justOrEmpty(id))
            .zipWhen(instance -> deviceProductService
                .findById(Mono.justOrEmpty(instance.getProductId())), DeviceInfo::of)
            .switchIfEmpty(Mono.error(NotFoundException::new));
    }

    public Flux<Integer> syncStateBatch(Flux<List<String>> batch, boolean force) {
        return batch.flatMap(list -> Flux.fromIterable(list)
            .flatMap(registry::getDevice)
            .publishOn(Schedulers.parallel())
            .flatMap(operation -> {
                if (force) {
                    return operation.checkState().zipWith(Mono.just(operation.getDeviceId()));
                }
                return operation.getState().zipWith(Mono.just(operation.getDeviceId()));
            })
            .groupBy(Tuple2::getT1, Tuple2::getT2)
            .flatMap(group -> {
                @SuppressWarnings("all")
                DeviceState state = group.key() == null ? DeviceState.offline : DeviceState.of(group.key());
                return group.collectList()
                    .flatMap(idList -> getRepository()
                        .createUpdate()
                        .set(DeviceInstanceEntity::getState, state)
                        .where()
                        .in(DeviceInstanceEntity::getId, idList)
                        .execute());
            }));
    }


    /**
     * 同步设备状态
     *
     * @param deviceId 设备id集合
     * @param force    是否强制同步,将会检查设备的真实状态
     * @return 同步成功数量
     */
    public Flux<Integer> syncState(Flux<String> deviceId, boolean force) {
        return syncStateBatch(FluxUtils.bufferRate(deviceId, 800, Duration.ofSeconds(5)), force);

    }

    public Flux<ImportDeviceInstanceResult> doBatchImport(String fileUrl) {
        return deviceProductService
            .createQuery()
            //如果指定了机构,则只查询机构内的设备型号
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
            .buffer(50)
            .flatMap(list -> this.save(Flux.fromIterable(list)))
            .map(ImportDeviceInstanceResult::success)
            .onErrorResume(err -> Mono.just(ImportDeviceInstanceResult.error(err)))
            .doOnEach(ReactiveLogger.on(SignalType.CANCEL, (ctx, signal) -> {
                log.warn("用户取消导入设备实例:{}", fileUrl);
            }))
            ;
    }

    private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    @SneakyThrows
    public Mono<Void> doExport(ServerHttpResponse response, QueryParam queryParam, String fileName) {
        response.getHeaders()
            .set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=".concat(
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.displayName())));

        queryParam.setPaging(false);

        return response.writeWith(Flux.create(sink -> {
                OutputStream outputStream = new OutputStream() {
                    @Override
                    public void write(byte[] b) {
                        sink.next(bufferFactory.wrap(b));
                    }

                    @Override
                    public void write(int b) {
                        sink.next(bufferFactory.wrap(new byte[]{(byte) b}));
                    }

                    @Override
                    public void close() {
                        sink.complete();
                    }
                };
                ExcelWriter excelWriter = EasyExcel.write(outputStream, DeviceInstanceImportExportEntity.class).build();
                WriteSheet writeSheet = EasyExcel.writerSheet().build();
                sink.onCancel(query(queryParam)
                    .map(entity -> FastBeanCopier.copy(entity, new DeviceInstanceImportExportEntity()))
                    .buffer(100)
                    .doOnNext(list -> excelWriter.write(list, writeSheet))
                    .doFinally(s -> excelWriter.finish())
                    .doOnError(sink::error)
                    .subscribe());
            })
        );
    }

    public Mono<ESDevicePropertiesEntity> getProperty(String deviceId, String property) {
        return createQuery().where(DeviceInstanceEntity::getId, deviceId)
            .fetchOne()
            .map(DeviceInstanceEntity::getProductId)
            .flatMap(productId -> getElasticSearchProperty(productId, deviceId, property)
                .singleOrEmpty()
            );
    }

    public Flux<ESDevicePropertiesEntity> getProperties(String productId, String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> {
                return Flux.merge(metadata.getProperties()
                    .stream()
                    .map(property -> getElasticSearchProperty(productId, deviceId, property.getId()))
                    .collect(Collectors.toList()));
            })
            ;
    }

    private Flux<ESDevicePropertiesEntity> getElasticSearchProperty(String productId, String deviceId, String property) {
        QueryParam queryParam = Query.of()
            .and(ESDevicePropertiesEntity::getDeviceId, deviceId)
            .and(ESDevicePropertiesEntity::getProperty, property)
            .doPaging(0, 1)
            .getParam();
        queryParam.setSorts(Collections.singletonList(new Sort("timestamp")));
        return elasticSearchService
            .query(DeviceEventIndex.getDevicePropertiesIndex(productId), queryParam, ESDevicePropertiesEntity.class);
    }

    private Mono<Void> downloadFile(ServerHttpResponse response, File file, String fileName) {
        ZeroCopyHttpOutputMessage zeroCopyHttpOutputMessage = (ZeroCopyHttpOutputMessage) response;
        try {
            response.getHeaders()
                .set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=".concat(
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8.displayName())));
            return zeroCopyHttpOutputMessage.writeWith(file, 0, file.length());
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException();
        }
    }

}
