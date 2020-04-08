package org.jetlinks.community.device.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.message.DeviceMessageUtils;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.*;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.device.entity.excel.DeviceInstanceImportExportEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.*;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.EncodableMessage;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric.devicePropertyMetric;

@Service
@Slf4j
public class LocalDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private LocalDeviceProductService deviceProductService;

    @Autowired
    private MessageGateway messageGateway;

    @Autowired
    private TimeSeriesManager timeSeriesManager;

    @Autowired
    @SuppressWarnings("all")
    private ReactiveRepository<DeviceTagEntity, String> tagRepository;


    @Override
    public Mono<SaveResult> save(Publisher<DeviceInstanceEntity> entityPublisher) {
        return Flux.from(entityPublisher)
            .doOnNext(instance -> instance.setState(null))
            .as(super::save);
    }

    /**
     * 获取设备所有信息
     *
     * @param id 设备ID
     * @return 设备详情信息
     */
    @Deprecated
    public Mono<DeviceAllInfoResponse> getDeviceAllInfo(String id) {

        return findById(id)//设备信息
            .zipWhen(instance -> deviceProductService.findById(instance.getProductId()), DeviceInfo::of) //产品型号信息
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .zipWhen(deviceInfo -> getDeviceRunRealInfo(id), DeviceAllInfoResponse::of) //设备运行状态
            .zipWhen(info -> getDeviceLatestProperties(id).collectList(), DeviceAllInfoResponse::ofProperties) //设备属性
            .zipWhen(info -> {
                    DeviceMetadata deviceMetadata = new JetLinksDeviceMetadata(JSON.parseObject(info.getDeviceInfo().getDeriveMetadata()));
                    return getEventCounts(deviceMetadata.getEvents(), id, info.getDeviceInfo().getProductId()); //事件数量统计
                },
                DeviceAllInfoResponse::ofEventCounts)
            ;
    }

    /**
     * 获取设备事件上报次数
     *
     * @param events    设备事件元数据
     * @param deviceId  设备Id
     * @param productId 型号id
     * @return
     */
    private Mono<Map<String, Integer>> getEventCounts(List<EventMetadata> events, String deviceId, String productId) {
        return Flux.merge(
            events
                .stream()
                .map(Metadata::getId)
                .map(eventId -> Query.of()
                    .where("deviceId", deviceId)
                    .execute(timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceEventMetric(productId, eventId))::count)
                    .map(count -> Tuples.of(eventId, count)))
                .collect(Collectors.toList())
        ).collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2));
    }


    /**
     * 发布设备到设备注册中心
     *
     * @param id 设备ID
     * @return 发布结果
     */
    public Mono<DeviceDeployResult> deploy(String id) {
        return findById(id)
            .flux()
            .as(this::deploy)
            .singleOrEmpty();
    }

    /**
     * 批量发布设备到设备注册中心
     *
     * @param flux 设备实例流
     * @return 发布数量
     */
    public Flux<DeviceDeployResult> deploy(Flux<DeviceInstanceEntity> flux) {
        return flux
            .flatMap(instance -> registry
                .register(instance.toDeviceInfo())
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

    /**
     * 取消发布(取消激活),取消后,设备无法再连接到服务. 注册中心也无法再获取到该设备信息.
     *
     * @param id 设备ID
     * @return 取消结果
     */
    public Mono<Integer> cancelDeploy(String id) {
        return findById(Mono.just(id))
            .flatMap(product -> registry
                .unregisterDevice(id)
                .then(createUpdate()
                    .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                    .where(DeviceInstanceEntity::getId, id)
                    .execute()));
    }

    public Mono<DeviceDetail> getDeviceDetail(String deviceId) {
        return this.findById(deviceId)
            .zipWhen(
                //合并设备和型号信息
                (device) -> deviceProductService.findById(device.getProductId()),
                (device, product) -> new DeviceDetail().with(device).with(product)
            ).flatMap(detail -> registry
                .getDevice(deviceId)
                .flatMap(
                    operator -> operator.checkState() //检查设备的真实状态,设备已经离线,但是数据库状态未及时更新的.
                        .map(DeviceState::of)
                        .filter(state -> state != detail.getState())
                        .doOnNext(detail::setState)
                        .flatMap(state -> createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where(DeviceInstanceEntity::getId, deviceId)
                            .execute())
                        .thenReturn(operator))
                .flatMap(detail::with)
                .defaultIfEmpty(detail))
            //设备标签信息
            .flatMap(detail -> tagRepository
                .createQuery()
                .where(DeviceTagEntity::getDeviceId, deviceId)
                .fetch()
                .collectList()
                .map(detail::with)
                .defaultIfEmpty(detail));
    }

    public Mono<DeviceState> getDeviceState(String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(DeviceOperator::checkState)
            .flatMap(state -> {
                DeviceState deviceState = DeviceState.of(state);
                return createUpdate().set(DeviceInstanceEntity::getState, deviceState)
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .thenReturn(deviceState);
            })
            .defaultIfEmpty(DeviceState.notActive);
    }

    @Deprecated
    public Mono<DeviceRunInfo> getDeviceRunInfo(String deviceId) {
        return getDeviceRunRealInfo(deviceId);
    }

    @Deprecated
    private Mono<DeviceRunInfo> getDeviceRunRealInfo(String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(deviceOperator -> Mono.zip(
                deviceOperator.getOnlineTime().switchIfEmpty(Mono.just(0L)),// 1
                deviceOperator.getOfflineTime().switchIfEmpty(Mono.just(0L)),// 2
                deviceOperator.checkState()
                    .switchIfEmpty(deviceOperator.getState())
                    .map(DeviceState::of)
                    .defaultIfEmpty(DeviceState.notActive),// 3
                deviceOperator.getConfig(DeviceConfigKey.metadata).switchIfEmpty(Mono.just("")),//4
                deviceOperator.getConfig(DeviceConfigKey.productId).switchIfEmpty(Mono.just(""))//5
                ).map(tuple4 -> DeviceRunInfo.of(
                tuple4.getT1(), //1. 上线时间
                tuple4.getT2(), //2. 离线时间
                tuple4.getT3(), //3. 状态
                tuple4.getT4(),  //4. 设备模型元数据
                tuple4.getT5() //5. 设备类型ID
                )
                ).flatMap(deviceRunInfo -> createUpdate()
                    .set(DeviceInstanceEntity::getState, deviceRunInfo.getState())
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .thenReturn(deviceRunInfo))
            );
    }


    public Mono<PagerResult<DevicePropertiesEntity>> queryDeviceProperties(String deviceId, QueryParamEntity entity) {
        return registry.getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> timeSeriesManager
                .getService(devicePropertyMetric(productId))
                .queryPager(entity.and("deviceId", TermType.eq, deviceId), data -> data.as(DevicePropertiesEntity.class)))
            .defaultIfEmpty(PagerResult.empty());
    }

    public Mono<PagerResult<Map<String, Object>>> queryDeviceEvent(String deviceId, String eventId, QueryParamEntity entity, boolean format) {
        return registry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId).zipWith(operator.getMetadata()))
            .flatMap(tp -> timeSeriesManager
                .getService(DeviceTimeSeriesMetric.deviceEventMetric(tp.getT1(), eventId))
                .queryPager(entity.and("deviceId", TermType.eq, deviceId), data -> {
                    if (!format) {
                        return data.getData();
                    }
                    Map<String, Object> formatData = new HashMap<>(data.getData());
                    tp.getT2()
                        .getEvent(eventId)
                        .ifPresent(eventMetadata -> {
                            DataType type = eventMetadata.getType();
                            if (type instanceof ObjectType) {
                                @SuppressWarnings("all")
                                Map<String, Object> val = (Map<String, Object>) type.format(formatData);
                                val.forEach((k, v) -> formatData.put(k + "_format", v));
                            } else {
                                formatData.put("value_format", type.format(data.get("value")));
                            }
                        });
                    return formatData;
                })).defaultIfEmpty(PagerResult.empty());
    }

    public Mono<DevicePropertiesEntity> getDeviceLatestProperty(String deviceId, String property) {
        return registry
            .getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> doGetLatestDeviceProperty(productId, deviceId, property));
    }

    public Flux<DevicePropertiesEntity> getDeviceLatestProperties(String deviceId) {
        return registry.getDevice(deviceId)
            .flatMap(operator -> Mono.zip(operator.getMetadata(), operator.getSelfConfig(DeviceConfigKey.productId)))
            .flatMapMany(zip -> Flux.merge(zip.getT1().getProperties()
                .stream()
                .map(property -> doGetLatestDeviceProperty(zip.getT2(), deviceId, property.getId()))
                .collect(Collectors.toList())));
    }

    private Mono<DevicePropertiesEntity> doGetLatestDeviceProperty(String productId, String deviceId, String property) {
        return Query.of()
            .and(DevicePropertiesEntity::getDeviceId, deviceId)
            .and(DevicePropertiesEntity::getProperty, property)
            .doPaging(0, 1)
            .execute(timeSeriesManager.getService(devicePropertyMetric(productId))::query)
            .map(data -> data.as(DevicePropertiesEntity.class))
            .singleOrEmpty();
    }

    public Mono<PagerResult<DeviceOperationLogEntity>> queryDeviceLog(String deviceId, QueryParamEntity entity) {
        return registry.getDevice(deviceId)
            .flatMap(operator -> operator.getSelfConfig(DeviceConfigKey.productId))
            .flatMap(productId -> timeSeriesManager
                .getService(DeviceTimeSeriesMetric.deviceLogMetric(productId))
                .queryPager(entity.and("deviceId", TermType.eq, deviceId),
                    data -> data.as(DeviceOperationLogEntity.class)))
            .defaultIfEmpty(PagerResult.empty());
    }

    @PostConstruct
    public void init() {

        //订阅设备上下线
        FluxUtils.bufferRate(messageGateway
            .subscribe(Subscription.asList("/device/*/*/online", "/device/*/*/offline"), "device-state-synchronizer", false)
            .flatMap(message -> Mono.justOrEmpty(DeviceMessageUtils.convert(message))
                .map(DeviceMessage::getDeviceId)), 800, 200, Duration.ofSeconds(2))
            .publishOn(Schedulers.parallel())
            .concatMap(list -> syncStateBatch(Flux.just(list), false).reduce(Math::addExact))
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

        return batch
            .concatMap(list -> Flux.fromIterable(list)
                .publishOn(Schedulers.parallel())
                .flatMap(registry::getDevice)
                .flatMap(operation -> {
                    Mono<Byte> state = force ? operation.checkState() : operation.getState();
                    return Mono.zip(
                        state.defaultIfEmpty(org.jetlinks.core.device.DeviceState.offline),//状态
                        Mono.just(operation.getDeviceId()), //设备id
                        operation.getConfig(DeviceConfigKey.isGatewayDevice).defaultIfEmpty(false)//是否为网关设备
                    );
                })
                .collect(Collectors.groupingBy(Tuple2::getT1))
                .flatMapIterable(Map::entrySet)
                .flatMap(group -> {
                    DeviceState state = DeviceState.of(group.getKey());
                    return Mono.zip(
                        //批量修改设备状态
                        getRepository()
                            .createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where()
                            .in(DeviceInstanceEntity::getId, group.getValue().stream().map(Tuple3::getT2).collect(Collectors.toList()))
                            .execute()
                            .thenReturn(group.getValue().size())//mysql下可能不会返回更新数量
                        ,
                        //修改子设备状态
                        Flux.fromIterable(group.getValue())
                            .filter(Tuple3::getT3)
                            .map(Tuple3::getT2)
                            .collectList()
                            .filter(CollectionUtils::isNotEmpty)
                            .flatMap(parents ->
                                getRepository()
                                    .createUpdate()
                                    .set(DeviceInstanceEntity::getState, state)
                                    .where()
                                    .in(DeviceInstanceEntity::getParentId, parents)
                                    .execute())
                            .defaultIfEmpty(0),
                        Math::addExact);
                }));
    }


    @Subscribe("/device/*/*/message/children/*/register")
    public Mono<Void> autoBindChildrenDevice(ChildDeviceMessage message) {
        String childId = message.getChildDeviceId();
        Message childMessage = message.getChildDeviceMessage();
        if (childMessage instanceof DeviceRegisterMessage) {
            return registry.getDevice(message.getDeviceId())
                .flatMap(DeviceOperator::getState)
                .flatMap(state -> createUpdate()
                    .set(DeviceInstanceEntity::getParentId, message.getDeviceId())
                    .set(DeviceInstanceEntity::getState, DeviceState.of(state))
                    .where(DeviceInstanceEntity::getId, childId)
                    .execute()
                    .then(registry
                        .getDevice(childId)
                        .flatMap(dev -> dev.setConfig(DeviceConfigKey.parentGatewayId, message.getDeviceId())))
                    .then());
        }
        return Mono.empty();
    }

    @Subscribe("/device/*/*/message/children/*/unregister")
    public Mono<Void> autoUnbindChildrenDevice(ChildDeviceMessage message) {
        String childId = message.getChildDeviceId();
        Message childMessage = message.getChildDeviceMessage();
        if (childMessage instanceof DeviceUnRegisterMessage) {

            return registry.getDevice(childId)
                .flatMap(dev -> dev
                    .removeConfig(DeviceConfigKey.parentGatewayId.getKey())
                    .then(dev.checkState()))
                .flatMap(state -> createUpdate()
                    .setNull(DeviceInstanceEntity::getParentId)
                    .set(DeviceInstanceEntity::getState, DeviceState.of(state))
                    .where(DeviceInstanceEntity::getId, childId)
                    .execute()
                    .then());


        }
        return Mono.empty();
    }


}
