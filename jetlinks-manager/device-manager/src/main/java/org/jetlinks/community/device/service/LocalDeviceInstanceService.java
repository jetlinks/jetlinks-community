package org.jetlinks.community.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.dml.Terms;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceFeature;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.response.ResetDeviceConfigurationResult;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.utils.CyclicDependencyChecker;
import org.reactivestreams.Publisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService deviceProductService;

    private final DeviceConfigMetadataManager metadataManager;

    @SuppressWarnings("all")
    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    public LocalDeviceInstanceService(DeviceRegistry registry,
                                      LocalDeviceProductService deviceProductService,
                                      DeviceConfigMetadataManager metadataManager,
                                      @SuppressWarnings("all")
                                      ReactiveRepository<DeviceTagEntity, String> tagRepository) {
        this.registry = registry;
        this.deviceProductService = deviceProductService;
        this.metadataManager = metadataManager;
        this.tagRepository = tagRepository;
    }


    @Override
    public Mono<SaveResult> save(Publisher<DeviceInstanceEntity> entityPublisher) {
        return Flux.from(entityPublisher)
                   .doOnNext(instance -> instance.setState(null))
                   .as(super::save);
    }

    private Flux<DeviceInstanceEntity> findByProductId(String productId) {
        return createQuery()
            .and(DeviceInstanceEntity::getProductId, productId)
            .fetch();
    }

    private Set<String> getProductConfigurationProperties(DeviceProductEntity product) {
        if (MapUtils.isNotEmpty(product.getConfiguration())) {
            return product.getConfiguration()
                          .keySet();
        }
        return new HashSet<>();
    }

    private Mono<Map<String, Object>> resetConfiguration(DeviceProductEntity product, DeviceInstanceEntity device) {
        return metadataManager
            .getProductConfigMetadataProperties(product.getId())
            .defaultIfEmpty(getProductConfigurationProperties(product))
            .flatMap(set -> {
                if (set.size() > 0) {
                    if (MapUtils.isNotEmpty(device.getConfiguration())) {
                        set.forEach(device.getConfiguration()::remove);
                    }
                    //重置注册中心里的配置
                    return registry
                        .getDevice(device.getId())
                        .flatMap(opts -> opts.removeConfigs(set))
                        .then();
                }
                return Mono.empty();
            })
            .then(
                //更新数据库
                createUpdate()
                    .set(device::getConfiguration)
                    .where(device::getId)
                    .execute()
            )
            .then(Mono.fromSupplier(device::getConfiguration));
    }

    /**
     * 重置设备配置
     *
     * @param deviceId 设备ID
     * @return 重置后的配置
     * @since 1.2
     */
    public Mono<Map<String, Object>> resetConfiguration(String deviceId) {
        return this
            .findById(deviceId)
            .zipWhen(device -> deviceProductService.findById(device.getProductId()))
            .flatMap(tp2 -> {
                DeviceProductEntity product = tp2.getT2();
                DeviceInstanceEntity device = tp2.getT1();
                return resetConfiguration(product, device);
            })
            .defaultIfEmpty(Collections.emptyMap());
    }

    public Mono<Long> resetConfiguration(Flux<String> payload) {
        return payload
            .flatMap(deviceId -> resetConfiguration(deviceId)).count();
    }

    /**
     * 重置设备配置信息(根据产品批量重置，性能欠佳，慎用)
     *
     * @param productId 产品ID
     * @return 数量
     */
    public Flux<ResetDeviceConfigurationResult> resetConfigurationByProductId(String productId) {
        return deviceProductService
            .findById(productId)
            .flatMapMany(product -> this
                .findByProductId(productId)
                .flatMap(device -> {
                    return resetConfiguration(product, device)
                        .thenReturn(ResetDeviceConfigurationResult
                            .success(SaveResult.of(0, 1)))
                        .onErrorResume(throwable -> {
                            String message = device.getId() + ":" + throwable.getMessage();
                            return Mono.just(ResetDeviceConfigurationResult.error(message));
                        });
                })
            );
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
                .flatMap(deviceOperator -> deviceOperator
                    .getState()
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
            .flatMap(all -> Flux
                .fromIterable(all)
                .groupBy(DeviceInstanceEntity::getState)
                .flatMap(group -> group
                    .map(DeviceInstanceEntity::getId)
                    .collectList()
                    .flatMap(list -> createUpdate()
                        .where()
                        .set(DeviceInstanceEntity::getState, group.key())
                        .set(DeviceInstanceEntity::getRegistryTime, new Date())
                        .in(DeviceInstanceEntity::getId, list)
                        .execute()
                        .map(r -> DeviceDeployResult.success(list.size()))
                        .onErrorResume(err -> Mono.just(DeviceDeployResult.error(err.getMessage()))))))
            ;
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
     * 注销设备,取消后,设备无法再连接到服务. 注册中心也无法再获取到该设备信息.
     *
     * @param id 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(String id) {
        return this.findById(Mono.just(id))
                   .flatMap(device -> registry
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

    protected Mono<DeviceDetail> createDeviceDetail(DeviceProductEntity product,
                                                    DeviceInstanceEntity device,
                                                    List<DeviceTagEntity> tags) {

        DeviceDetail detail = new DeviceDetail().with(product).with(device).with(tags);
        return Mono
            .zip(
                //设备信息
                registry
                    .getDevice(device.getId())
                    .flatMap(operator -> operator
                        //检查设备的真实状态,可能出现设备已经离线,但是数据库状态未及时更新的.
                        .checkState()
                        .map(DeviceState::of)
                        //检查失败,则返回原始状态
                        .onErrorReturn(device.getState())
                        //如果状态不一致,则需要更新数据库中的状态
                        .filter(state -> state != detail.getState())
                        .doOnNext(detail::setState)
                        .flatMap(state -> createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where(DeviceInstanceEntity::getId, device.getId())
                            .execute())
                        .thenReturn(operator)),
                //配置定义
                metadataManager
                    .getDeviceConfigMetadata(device.getId())
                    .flatMapIterable(ConfigMetadata::getProperties)
                    .collectList(),
                detail::with
            )
            //填充详情信息
            .flatMap(Function.identity())
            .switchIfEmpty(
                Mono.defer(() -> {
                    //如果设备注册中心里没有设备信息,并且数据库里的状态不是未激活.
                    //可能是因为注册中心信息丢失,修改数据库中的状态信息.
                    if (detail.getState() != DeviceState.notActive) {
                        return createUpdate()
                            .set(DeviceInstanceEntity::getState, DeviceState.notActive)
                            .where(DeviceInstanceEntity::getId, detail.getId())
                            .execute()
                            .thenReturn(detail.notActive());
                    }
                    return Mono.just(detail.notActive());
                }).thenReturn(detail))
            .onErrorResume(err -> {
                log.warn("get device detail error", err);
                return Mono.just(detail);
            });

    }

    public Mono<DeviceDetail> getDeviceDetail(String deviceId) {
        return this
            .findById(deviceId)
            .zipWhen(device -> deviceProductService.findById(device.getProductId()))//合并型号
            .zipWith(tagRepository
                         .createQuery()
                         .where(DeviceTagEntity::getDeviceId, deviceId)
                         .fetch()
                         .collectList()
                         .defaultIfEmpty(Collections.emptyList()) //合并标签
                , (left, right) -> Tuples.of(left.getT2(), left.getT1(), right))
            .flatMap(tp3 -> createDeviceDetail(tp3.getT1(), tp3.getT2(), tp3.getT3()));
    }

    public Mono<DeviceState> getDeviceState(String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::checkState)
            .flatMap(state -> {
                DeviceState deviceState = DeviceState.of(state);
                return this
                    .createUpdate()
                    .set(DeviceInstanceEntity::getState, deviceState)
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .thenReturn(deviceState);
            })
            .defaultIfEmpty(DeviceState.notActive);
    }

    public Flux<List<DeviceStateInfo>> syncStateBatch(Flux<List<String>> batch, boolean force) {

        return batch
            .concatMap(list -> Flux
                .fromIterable(list)
                .publishOn(Schedulers.parallel())
                .flatMap(id -> registry
                    .getDevice(id)
                    .flatMap(operator -> {
                        Mono<Byte> state = force
                            ? operator
                            .checkState()
                            .onErrorResume(err -> operator.getState())
                            : operator.getState();
                        return Mono
                            .zip(
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
                    List<String> deviceIdList = group
                        .getValue()
                        .stream()
                        .map(Tuple3::getT2)
                        .collect(Collectors.toList());
                    DeviceState state = DeviceState.of(group.getKey());
                    return
                        //批量修改设备状态
                        getRepository()
                            .createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where()
                            .in(DeviceInstanceEntity::getId, deviceIdList)
                            .when(state != DeviceState.notActive, where -> where.not(DeviceInstanceEntity::getState, DeviceState.notActive))
                            .execute()
                            .thenReturn(group.getValue().size())
                            .then(Mono.just(
                                deviceIdList
                                    .stream()
                                    .map(id -> DeviceStateInfo.of(id, state))
                                    .collect(Collectors.toList())
                            ));
                }))
            //更新状态不触发事件
            .as(EntityEventHelper::setDoNotFireEvent);
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
            .flatMap(deviceOperator -> deviceOperator
                .messageSender()
                .readProperty(property)
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .send()
                .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
                .reduceWith(LinkedHashMap::new, (main, map) -> {
                    main.putAll(map);
                    return main;
                })
                .flatMap(map -> {
                    Object value = map.get(property);
                    return deviceOperator
                        .getMetadata()
                        .map(deviceMetadata -> deviceMetadata
                            .getProperty(property)
                            .map(PropertyMetadata::getValueType)
                            .orElse(new StringType()))
                        .map(dataType -> DevicePropertiesEntity
                            .builder()
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

    private final CyclicDependencyChecker<DeviceInstanceEntity, Void> checker = CyclicDependencyChecker
        .of(DeviceInstanceEntity::getId, DeviceInstanceEntity::getParentId, this::findById);

    public Mono<Void> checkCyclicDependency(DeviceInstanceEntity device) {
        return checker.check(device);
    }

    public Mono<Void> checkCyclicDependency(String id, String parentId) {
        DeviceInstanceEntity instance = new DeviceInstanceEntity();
        instance.setId(id);
        instance.setParentId(parentId);
        return checker.check(instance);
    }

    public Mono<Void> mergeConfiguration(String deviceId,
                                         Map<String, Object> configuration,
                                         Function<ReactiveUpdate<DeviceInstanceEntity>,
                                             ReactiveUpdate<DeviceInstanceEntity>> updateOperation) {
        if (MapUtils.isEmpty(configuration)) {
            return Mono.empty();
        }
        return this
            .findById(deviceId)
            .flatMap(device -> {
                //合并更新配置
                device.mergeConfiguration(configuration);
                return createUpdate()
                    .set(device::getConfiguration)
                    .set(device::getFeatures)
                    .set(device::getDeriveMetadata)
                    .as(updateOperation)
                    .where(device::getId)
                    .execute();
            })
            .then(
                //更新缓存里到信息
                registry
                    .getDevice(deviceId)
                    .flatMap(device -> device.setConfigs(configuration))
            )
            .then();

    }

    /**
     * 删除设备后置处理,解绑子设备和网关,并在注册中心取消激活已激活设备.
     */
    private Flux<Void> deletedHandle(Flux<DeviceInstanceEntity> devices) {
        return devices.filter(device -> !StringUtils.isEmpty(device.getParentId()))
            .groupBy(DeviceInstanceEntity::getParentId)
            .flatMap(group -> {
                String parentId = group.key();
                return group.flatMap(child -> registry.getDevice(child.getId())
                            .flatMap(device -> device.removeConfig(DeviceConfigKey.parentGatewayId.getKey()).thenReturn(device))
                    )
                    .as(childrenDeviceOp -> registry.getDevice(parentId)
                        .flatMap(gwOperator -> gwOperator.getProtocol()
                            .flatMap(protocolSupport -> protocolSupport.onChildUnbind(gwOperator, childrenDeviceOp))
                        )
                    );
            })
            // 取消激活
            .thenMany(
                devices.filter(device -> device.getState() != DeviceState.notActive)
                    .flatMap(device -> registry.unregisterDevice(device.getId()))
            );
    }

    @EventListener
    public void deletedHandle(EntityDeletedEvent<DeviceInstanceEntity> event) {
        event.async(
            this.deletedHandle(Flux.fromIterable(event.getEntity())).then()
        );
    }
}
