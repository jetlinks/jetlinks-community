package org.jetlinks.community.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.response.DeviceInfo;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.FluxUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
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
    private EventBus eventBus;

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
     * 重置设备配置
     *
     * @param deviceId 设备ID
     * @return 重置后的配置
     * @since 1.2
     */
    public Mono<Map<String, Object>> resetConfiguration(String deviceId) {
        return findById(deviceId)
            .flatMap(device ->
                Mono.defer(() -> {
                    if (!MapUtils.isEmpty(device.getConfiguration())) {
                        //重置注册中心里的配置
                        return registry.getDevice(deviceId)
                            .flatMap(opts -> opts.removeConfigs(device.getConfiguration().keySet()))
                            .then();
                    }
                    return Mono.empty();
                }).then(
                    //更新数据库
                    createUpdate()
                        .set(DeviceInstanceEntity::getConfiguration, new HashMap<>())
                        .where(DeviceInstanceEntity::getId, deviceId)
                        .execute()
                ).then(
                    //获取产品信息的配置
                    deviceProductService
                        .findById(device.getProductId())
                        .flatMap(product -> Mono.justOrEmpty(product.getConfiguration()))
                ))
            .defaultIfEmpty(Collections.emptyMap())
            ;
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

    /**
     * 批量注销设备
     *
     * @param ids 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(Publisher<String> ids) {
        return Flux.from(ids)
            .flatMap(id -> registry.unregisterDevice(id).thenReturn(id))
            .collectList()
            .flatMap(list -> createUpdate()
                .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                .where().in(DeviceInstanceEntity::getId, list)
                .execute());
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
                .switchIfEmpty(Mono.defer(() -> {
                    if (detail.getState() != DeviceState.notActive) {
                        return createUpdate()
                            .set(DeviceInstanceEntity::getState, DeviceState.notActive)
                            .where(DeviceInstanceEntity::getId, deviceId)
                            .execute()
                            .thenReturn(detail.notActive());
                    }
                    return Mono.just(detail.notActive());
                })))
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

        org.jetlinks.core.event.Subscription subscription = org.jetlinks.core.event.Subscription.of(
            "device-state-synchronizer",
            new String[]{
                "/device/*/*/online",
                "/device/*/*/offline"
            },
            Subscription.Feature.local
        );

        //订阅设备上下线消息,同步数据库中的设备状态,
        //最小间隔800毫秒,最大缓冲数量500,最长间隔2秒.
        //如果2条消息间隔大于0.8秒则不缓冲直接更新
        //否则缓冲,数量超过500后批量更新
        //无论缓冲区是否超过500条,都每2秒更新一次.
        FluxUtils.bufferRate(eventBus
                .subscribe(subscription,DeviceMessage.class)
                .map(DeviceMessage::getDeviceId),
            800, Integer.getInteger("device.state.sync.batch", 500), Duration.ofSeconds(2))
            .publishOn(Schedulers.parallel())
            .concatMap(list -> syncStateBatch(Flux.just(list), false).map(List::size))
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .subscribe((i) -> log.info("同步设备状态成功:{}", i));
    }

    public Mono<DeviceInfo> getDeviceInfoById(String id) {
        return findById(Mono.justOrEmpty(id))
            .zipWhen(instance -> deviceProductService
                .findById(Mono.justOrEmpty(instance.getProductId())), DeviceInfo::of)
            .switchIfEmpty(Mono.error(NotFoundException::new));
    }

    public Flux<List<DeviceStateInfo>> syncStateBatch(Flux<List<String>> batch, boolean force) {

        return batch
            .concatMap(list -> Flux.fromIterable(list)
                .publishOn(Schedulers.parallel())
                .flatMap(id ->
                    registry.getDevice(id)
                        .flatMap(operator -> {
                            Mono<Byte> state = force ? operator.checkState() : operator.getState();
                            return Mono.zip(
                                state.defaultIfEmpty(org.jetlinks.core.device.DeviceState.offline),//状态
                                Mono.just(operator.getDeviceId()), //设备id
                                operator.getConfig(DeviceConfigKey.isGatewayDevice).defaultIfEmpty(false)//是否为网关设备
                            );
                        })
                        //注册中心里不存在设备就认为是未激活.
                        .defaultIfEmpty(Tuples.of(org.jetlinks.core.device.DeviceState.noActive, id, false)))
                .collect(Collectors.groupingBy(Tuple2::getT1))
                .flatMapIterable(Map::entrySet)
                .flatMap(group -> {
                    List<String> deviceId=group.getValue().stream().map(Tuple3::getT2).collect(Collectors.toList());
                    DeviceState state = DeviceState.of(group.getKey());
                    return Mono.zip(
                        //批量修改设备状态
                        getRepository()
                            .createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where()
                            .in(DeviceInstanceEntity::getId,deviceId)
                            .execute()
                            .thenReturn(group.getValue().size()),
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
                            .defaultIfEmpty(0))
                        .thenReturn(deviceId.stream().map(id->DeviceStateInfo.of(id,state)).collect(Collectors.toList()));
                }));
    }

    private static <R extends DeviceMessageReply, T> Function<R, Mono<T>> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return Mono.justOrEmpty(function.apply(reply));
        };
    }

    //获取标准设备属性
    @SneakyThrows
    public Mono<DevicePropertiesEntity> readAndConvertProperty(String deviceId,
                                                               String property) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMap(deviceOperator -> deviceOperator.messageSender()
                .readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send()
                .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
                .reduceWith(LinkedHashMap::new, (main, map) -> {
                    main.putAll(map);
                    return main;
                })
                .flatMap(map -> {
                    Object value = map.get(property);
                    return deviceOperator.getMetadata()
                        .map(deviceMetadata -> deviceMetadata.getProperty(property)
                            .map(PropertyMetadata::getValueType)
                            .orElse(new StringType()))
                        .map(dataType -> DevicePropertiesEntity.builder()
                            .deviceId(deviceId)
                            .productId(property)
                            .build()
                            .withValue(dataType, value));
                }));

    }

    //设置设备属性
    @SneakyThrows
    public Mono<Map<String, Object>> writeProperties(String deviceId,
                                                     Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .map(operator -> operator
                .messageSender()
                .writeProperty()
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .write(properties)
            )
            .flatMapMany(WritePropertyMessageSender::send)
            .flatMap(mapReply(WritePropertyMessageReply::getProperties))
            .reduceWith(LinkedHashMap::new, (main, map) -> {
                main.putAll(map);
                return main;
            });
    }

    //设备功能调用
    @SneakyThrows
    public Flux<?> invokeFunction(String deviceId,
                                  String functionId,
                                  Map<String, Object> properties) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
            .flatMap(operator -> operator
                .messageSender()
                .invokeFunction(functionId)
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .setParameter(properties)
                .validate()
            )
            .flatMapMany(FunctionInvokeMessageSender::send)
            .flatMap(mapReply(FunctionInvokeMessageReply::getOutput));
    }


    @Subscribe("/device/*/*/register")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> autoRegisterDevice(DeviceRegisterMessage message) {
        return registry
            .getDevice(message.getDeviceId())
            .switchIfEmpty(Mono.defer(() -> {
                //自动注册
                return doAutoRegister(message);
            }))
            .then();
    }

    private Mono<DeviceOperator> doAutoRegister(DeviceRegisterMessage message) {
        //自动注册
        return Mono.zip(
            Mono.justOrEmpty(message.getDeviceId()),//1. 设备ID
            Mono.justOrEmpty(message.getHeader("deviceName")).map(String::valueOf),//2. 设备名称
            Mono.justOrEmpty(message.getHeader("productId").map(String::valueOf)), //3. 产品ID
            Mono.justOrEmpty(message.getHeader("productId").map(String::valueOf)) //4. 产品
                .flatMap(deviceProductService::findById),
            Mono.justOrEmpty(message.getHeader("configuration").map(Map.class::cast).orElse(new HashMap()))//配置信息
        ).flatMap(tps -> {
            DeviceInstanceEntity instance = new DeviceInstanceEntity();
            instance.setId(tps.getT1());
            instance.setName(tps.getT2());
            instance.setProductId(tps.getT3());
            instance.setProductName(tps.getT4().getName());
            instance.setConfiguration(tps.getT5());
            instance.setCreateTimeNow();
            instance.setCreatorId(tps.getT4().getCreatorId());
            instance.setOrgId(tps.getT4().getOrgId());
            instance.setState(DeviceState.online);
            return super
                .save(Mono.just(instance))
                .thenReturn(instance)
                .flatMap(device -> registry.register(device.toDeviceInfo()));
        });
    }

    /**
     * 通过订阅子设备注册消息,自动绑定子设备到网关设备
     *
     * @param message 子设备消息
     * @return void
     */
    @Subscribe("/device/*/*/message/children/*/register")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> autoBindChildrenDevice(ChildDeviceMessage message) {
        String childId = message.getChildDeviceId();
        Message childMessage = message.getChildDeviceMessage();
        if (childMessage instanceof DeviceRegisterMessage) {

            return registry
                .getDevice(childId)
                .switchIfEmpty(Mono.defer(() -> doAutoRegister(((DeviceRegisterMessage) childMessage))))
                .flatMap(dev -> dev.setConfig(DeviceConfigKey.parentGatewayId, message.getDeviceId()).thenReturn(dev))
                .flatMap(DeviceOperator::getState)
                .flatMap(state ->
                    createUpdate()
                        .set(DeviceInstanceEntity::getParentId, message.getDeviceId())
                        .set(DeviceInstanceEntity::getState, DeviceState.of(state))
                        .where(DeviceInstanceEntity::getId, childId)
                        .execute()
                ).then();
        }
        return Mono.empty();
    }

    /**
     * 通过订阅子设备注销消息,自动解绑子设备
     *
     * @param message 子设备消息
     * @return void
     */
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
