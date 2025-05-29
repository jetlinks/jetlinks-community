/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TransactionManagers;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.TraceSourceException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.core.utils.CyclicDependencyChecker;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.events.DeviceDeployedEvent;
import org.jetlinks.community.device.events.DeviceUnregisterEvent;
import org.jetlinks.community.device.web.response.DeviceDeployResult;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.service.RelationService;
import org.jetlinks.community.relation.service.response.RelatedInfo;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.Function3;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService deviceProductService;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final DeviceConfigMetadataManager metadataManager;

    private final RelationService relationService;

    private final TransactionalOperator transactionalOperator;

    public LocalDeviceInstanceService(DeviceRegistry registry,
                                      LocalDeviceProductService deviceProductService,
                                      @SuppressWarnings("all")
                                      ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      DeviceConfigMetadataManager metadataManager,
                                      RelationService relationService,
                                      TransactionalOperator transactionalOperator) {
        this.registry = registry;
        this.deviceProductService = deviceProductService;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
        this.metadataManager = metadataManager;
        this.relationService = relationService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceInstanceEntity> entityPublisher) {
        return Flux
            .from(entityPublisher)
            .flatMap(instance -> {
                instance.setState(null);
                if (!StringUtils.hasText(instance.getId())) {
                    return handleCreateBefore(instance);
                }
                return registry
                    .getDevice(instance.getId())
                    .flatMap(DeviceOperator::getState)
                    .map(DeviceState::of)
                    .onErrorReturn(DeviceState.offline)
                    .defaultIfEmpty(DeviceState.notActive)
                    .doOnNext(instance::setState)
                    .thenReturn(instance);
            }, 16, 16)
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
        return this
            .findById(deviceId)
            .zipWhen(device -> deviceProductService.findById(device.getProductId()))
            .flatMap(tp2 -> {
                DeviceProductEntity product = tp2.getT2();
                DeviceInstanceEntity device = tp2.getT1();
                return Mono
                    .defer(() -> {
                        if (MapUtils.isNotEmpty(product.getConfiguration())) {
                            if (MapUtils.isNotEmpty(device.getConfiguration())) {
                                product.getConfiguration()
                                       .keySet()
                                       .forEach(device.getConfiguration()::remove);
                            }
                            //重置注册中心里的配置
                            return registry
                                .getDevice(deviceId)
                                .flatMap(opts -> opts.removeConfigs(product.getConfiguration().keySet()))
                                .then();
                        }
                        return Mono.empty();
                    })
                    .then(
                        Mono.defer(() -> {
                            //更新数据库
                            return createUpdate()
                                .when(device.getConfiguration() != null, update -> update.set(device::getConfiguration))
                                .when(device.getConfiguration() == null, update -> update.setNull(DeviceInstanceEntity::getConfiguration))
                                .where(device::getId)
                                .execute();
                        })
                    )
                    .then(Mono.fromSupplier(device::getConfiguration));
            })
            .defaultIfEmpty(Collections.emptyMap())
            ;
    }

    /**
     * 发布设备到设备注册中心
     *
     * @param id 设备ID
     * @return 发布结果
     */
    @Transactional(rollbackFor = Throwable.class, transactionManager = TransactionManagers.reactiveTransactionManager)
    public Mono<DeviceDeployResult> deploy(String id) {
        return this
            .findById(id)
            .flux()
            .as(flux -> deploy(flux, (err, device) -> Flux.error(err)))
            .singleOrEmpty();
    }


    /**
     * 批量发布设备到设备注册中心,并异常返回空
     *
     * @param flux 设备实例流
     * @return 发布数量
     */
    public Flux<DeviceDeployResult> deploy(Flux<DeviceInstanceEntity> flux) {
        return this
            .deploy(flux, this::retryDeploy);
//            .contextWrite(TraceSourceException.deepTraceContext());
    }

    /**
     * 批量发布设备到设备注册中心并指定异常
     *
     * @param flux 设备实例流
     * @return 发布数量
     */
    public Flux<DeviceDeployResult> deploy(Flux<DeviceInstanceEntity> flux,
                                           BiFunction<Throwable, List<DeviceInstanceEntity>, Flux<DeviceDeployResult>> fallback) {
        //设备回滚 key: deviceId value: 操作
        Map<String, Mono<Void>> rollback = new ConcurrentHashMap<>();
        List<Flux<DeviceDeployResult>> fail = new CopyOnWriteArrayList<>();

        return flux
            //添加回滚操作,用于再触发DeviceDeployedEvent事件执行失败时进行回滚.
            .flatMap(device -> registry
                .getDevice(device.getId())
                .onErrorResume(TraceSourceException.transfer("operation.device.deploy", device))
                .onErrorResume(err -> {
                    fail.add(deployFail(err, 1));
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    //设备之前没有注册的回滚操作(注销)
                    rollback.put(device.getId(), registry.unregisterDevice(device.getId()));
                }))
                .thenReturn(device), 32, 32)
            //发布到注册中心
            .flatMap(instance -> registry
                .register(instance.toDeviceInfo())
                .flatMap(deviceOperator -> deviceOperator
                    .checkState()//激活时检查设备状态
                    .onErrorReturn(org.jetlinks.core.device.DeviceState.offline)
                    .flatMap(r -> {
                        if (r.equals(org.jetlinks.core.device.DeviceState.unknown) ||
                            r.equals(org.jetlinks.core.device.DeviceState.noActive)) {
                            instance.setState(DeviceState.offline);
                            return deviceOperator.putState(org.jetlinks.core.device.DeviceState.offline);
                        }
                        instance.setState(DeviceState.of(r));
                        return Mono.just(true);
                    })
                    .flatMap(success -> success ? Mono.just(deviceOperator) : Mono.empty()))
                .as(EntityEventHelper::setDoNotFireEvent)
                .thenReturn(instance)
                //激活失败,返回错误,继续处理其他设备
                .onErrorResume(TraceSourceException.transfer("operation.device.deploy", instance))
                .onErrorResume(err -> {
                    fail.add(deployFail(err, 1));
                    return Mono.empty();
                }), 16, 16)
            .buffer(200)//每200条数据批量更新
            .publishOn(Schedulers.single())
            .concatMap(all -> Flux
                .fromIterable(all)
                .groupBy(DeviceInstanceEntity::getState)
                .flatMap(group -> group
                    // 检查协议相关配置的必填项
                    .flatMap(device -> validateConfiguration(device).thenReturn(device), 16, 16)
                    .map(DeviceInstanceEntity::getId)
                    .collectList()
                    .flatMap(list -> createUpdate()
                        .where()
                        .set(DeviceInstanceEntity::getState, group.key())
                        .set(DeviceInstanceEntity::getRegistryTime, System.currentTimeMillis())
                        .in(DeviceInstanceEntity::getId, list)
                        .is(DeviceInstanceEntity::getState, DeviceState.notActive)
                        .execute()
                        .map(r -> DeviceDeployResult.success(list.size()))))
                .as(EntityEventHelper::setDoNotFireEvent)
                //推送激活事件
                .flatMap(res -> DeviceDeployedEvent.of(all).publish(eventPublisher).thenReturn(res),
                         16, 16)
                //传递国际化上下文
                .as(LocaleUtils::transform)
                .as(transactionalOperator::transactional)
                .onErrorResume(err -> Flux
                    .fromIterable(all)
                    .mapNotNull(device -> rollback.get(device.getId()))
                    .flatMap(Function.identity())
                    .thenMany(fallback
                                  .apply(err, all)
                                  .switchIfEmpty(deployFail(err, all.size()))
                    )
                    .as(EntityEventHelper::setDoNotFireEvent)
                )
            )
            .concatWith(Flux.fromIterable(fail).flatMap(Function.identity()))
            ;
    }

    /**
     * 重试启用
     * 将批量启用失败的设备，再次尝试单个启用
     *
     * @param err    异常
     * @param device 设备列表
     * @return 结果
     */
    private Flux<DeviceDeployResult> retryDeploy(Throwable err,
                                                 List<DeviceInstanceEntity> device) {
        if (device == null || device.size() == 1) {
            // 单个启用错误时，直接返回错误结果
            return deployFail(err, 1);
        }
        return Flux.fromIterable(device)
                   // 启用单个设备
                   .flatMap(d -> this.deploy(Flux.just(d), this::retryDeploy), 8)
                   .groupBy(DeviceDeployResult::isSuccess)
                   .flatMap(group -> {
                       if (group.key()) {
                           return group
                               .count()
                               .map(success -> DeviceDeployResult.success(success.intValue()));
                       } else {
                           return group;
                       }
                   });
    }

    private Flux<DeviceDeployResult> deployFail(Throwable err,
                                                int total) {
        return Flux
            .zip(
                I18nSupportException.tryGetLocalizedMessageReactive(err),
                TraceSourceException.tryGetOperationLocalizedReactive(err).defaultIfEmpty(""),
                (msg, opt) -> new DeviceDeployResult(total,
                                                     false,
                                                     msg,
                                                     null,
                                                     TraceSourceException.tryGetSource(err),
                                                     opt)
            );
    }

    private Mono<Void> validateConfiguration(DeviceInstanceEntity device) {
        return metadataManager
            .getDeviceConfigMetadata(device.getId())
            .flatMap(configMetadata -> {
                List<String> property = configMetadata
                    .getProperties()
                    .stream()
                    .map(ConfigPropertyMetadata::getProperty)
                    .collect(Collectors.toList());
                return registry
                    .getProduct(device.getProductId())
                    .flatMap(product -> product.getConfigs(property))
                    .map(Values::getAllValues)
                    .defaultIfEmpty(new HashMap<>())
                    .map(conf -> {
                        if (MapUtils.isNotEmpty(device.getConfiguration())) {
                            return new CompositeMap<>(conf, device.getConfiguration());
                        }
                        return conf;
                    })
                    .zipWith(Mono.just(configMetadata));
            })
            .flatMap(tp2 -> DeviceConfigMetadataManager.validate(tp2.getT2(), tp2.getT1()))
            .filter(validateResult -> !validateResult.isSuccess())
            .flatMap(validateResult -> {
                if (log.isDebugEnabled()) {
                    log.debug(validateResult.getErrorMsg());
                }
                return Mono
                    .error(() -> new TraceSourceException("error.device_configuration_required_must_not_be_null")
                        .withSource("operation.device.deploy", device));
            })
            .contextWrite(Context.of(DeviceInstanceEntity.class, device))
            .then();
    }

    /**
     * 注销设备,取消后,设备无法再连接到服务. 注册中心也无法再获取到该设备信息.
     *
     * @param id 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(String id) {
        return this
            .unregisterDevice(Mono.just(id))
            .thenReturn(1);
    }

    /**
     * 批量注销设备
     *
     * @param ids 设备ID
     * @return 注销结果
     */
    public Mono<Integer> unregisterDevice(Publisher<String> ids) {
        return this
            .handleUnregister(ids)
            .count()
            .map(Long::intValue);
    }

    public Flux<DeviceDeployResult> handleUnregister(Publisher<String> ids) {
        return Flux
            .from(ids)
            .buffer(200)
            //先修改状态
            .flatMap(list -> this
                .findById(list)
                .collectList()
                .flatMap(devices -> DeviceUnregisterEvent.of(devices).publish(eventPublisher))
                .then(this
                          .createUpdate()
                          .set(DeviceInstanceEntity::getState, DeviceState.notActive.getValue())
                          .where().in(DeviceInstanceEntity::getId, list)
                          .execute()
                          //注销不触发事件,单独处理DeviceDeployedEvent
                          .as(EntityEventHelper::setDoNotFireEvent)
                          .thenReturn(list)
                ), 16)
            .flatMapIterable(Function.identity())
            //再注销
            .flatMap(id -> registry
                .getDevice(id)
                .flatMap(DeviceOperator::disconnect)
                .onErrorResume(err -> Mono.empty())
                .then(registry.unregisterDevice(id))
                .thenReturn(DeviceDeployResult.success(1))
                .onErrorResume(err -> Mono.just(DeviceDeployResult.error(err.getMessage(), id))))
            .as(EntityEventHelper::setDoNotFireEvent);
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .collectList()
                   .flatMap(list -> createDelete()
                       .where()
                       .in(DeviceInstanceEntity::getId, list)
                       .and(DeviceInstanceEntity::getState, DeviceState.notActive)
                       .execute());
    }

    private boolean hasContext(QueryParamEntity param, String key) {
        return param
            .getContext(key)
            .map(CastUtils::castBoolean)
            .orElse(true);
    }

    //分页查询设备详情列表
    public Mono<PagerResult<DeviceDetail>> queryDeviceDetail(QueryParamEntity entity) {

        return this
            .queryPager(entity)
            .filter(e -> CollectionUtils.isNotEmpty(e.getData()))
            .flatMap(result -> this
                .convertDeviceInstanceToDetail(result.getData(),
                                               hasContext(entity, "includeTags"),
                                               hasContext(entity, "includeBind"),
                                               hasContext(entity, "includeRelations"),
                                               hasContext(entity, "includeFirmwareInfos"))
                .collectList()
                .map(detailList -> PagerResult.of(result.getTotal(), detailList, entity)))
            .defaultIfEmpty(PagerResult.empty());
    }

    //查询设备详情列表
    public Flux<DeviceDetail> queryDeviceDetailList(QueryParamEntity entity) {
        return this
            .query(entity)
            .collectList()
            .flatMapMany(list -> this
                .convertDeviceInstanceToDetail(list,
                                               hasContext(entity, "includeTags"),
                                               hasContext(entity, "includeBind"),
                                               hasContext(entity, "includeRelations"),
                                               hasContext(entity, "includeFirmwareInfos")));
    }

    private Mono<Map<String, List<DeviceTagEntity>>> queryDeviceTagGroup(Collection<String> deviceIdList) {
        return tagRepository
            .createQuery()
            .where()
            .in(DeviceTagEntity::getDeviceId, deviceIdList)
            .fetch()
            .collect(Collectors.groupingBy(DeviceTagEntity::getDeviceId))
            .defaultIfEmpty(Collections.emptyMap());
    }

    private Flux<DeviceDetail> convertDeviceInstanceToDetail(List<DeviceInstanceEntity> instanceList,
                                                             boolean includeTag,
                                                             boolean includeBinds,
                                                             boolean includeRelations,
                                                             boolean includeFirmwareInfos) {
        if (CollectionUtils.isEmpty(instanceList)) {
            return Flux.empty();
        }
        List<String> deviceIdList = new ArrayList<>(instanceList.size());
        //按设备产品分组
        Map<String, List<DeviceInstanceEntity>> productGroup = instanceList
            .stream()
            .peek(device -> deviceIdList.add(device.getId()))
            .collect(Collectors.groupingBy(DeviceInstanceEntity::getProductId));
        //标签
        Mono<Map<String, List<DeviceTagEntity>>> tags = includeTag
            ? this.queryDeviceTagGroup(deviceIdList)
            : Mono.just(Collections.emptyMap());

        //关系信息
        Mono<Map<String, List<RelatedInfo>>> relations = includeRelations ? relationService
            .getRelationInfo(RelationObjectProvider.TYPE_DEVICE, deviceIdList)
            .collect(Collectors.groupingBy(RelatedInfo::getObjectId))
            .defaultIfEmpty(Collections.emptyMap())
            : Mono.just(Collections.emptyMap());


        return Mono
            .zip(
                //T1:查询出所有设备的产品信息
                deviceProductService
                    .findById(productGroup.keySet())
                    .collect(Collectors.toMap(DeviceProductEntity::getId, Function.identity())),
                //T2:查询出标签并按设备ID分组
                tags,
                //T3: 关系信息
                relations
            )
            .flatMapMany(tp5 -> Flux
                //遍历设备,将设备信息转为详情.
                .fromIterable(instanceList)
                .flatMap(instance -> this
                    .createDeviceDetail(
                        // 设备
                        instance
                        //产品
                        , tp5.getT1().get(instance.getProductId())
                        //标签
                        , tp5.getT2().get(instance.getId())
                        //关系信息
                        , tp5.getT3().get(instance.getId())
                    )
                    .as(MonoTracer.create("/LocalDeviceInstanceService/createDeviceDetail/" + instance.getId()))
                ))
            //createDeviceDetail是异步操作,可能导致顺序错乱.进行重新排序.
            .sort(Comparator.comparingInt(detail -> deviceIdList.indexOf(detail.getId())))
            .as(FluxTracer.create("/LocalDeviceInstanceService/convertDeviceInstanceToDetail"))
            ;
    }

    private Mono<DeviceDetail> createDeviceDetail(DeviceInstanceEntity device,
                                                  DeviceProductEntity product,
                                                  List<DeviceTagEntity> tags,
                                                  List<RelatedInfo> relations) {
        if (product == null) {
            log.warn("device [{}] product [{}] does not exists", device.getId(), device.getProductId());
            return Mono.empty();
        }
        DeviceDetail detail = new DeviceDetail()
            .with(product)
            .with(device)
            .with(tags)
            .withRelation(relations);

        return Mono
            .zip(
                //产品注册信息
                registry
                    .getProduct(product.getId()),
                //feature信息
                metadataManager
                    .getProductFeatures(product.getId())
                    .collectList())
            .flatMap(t2 -> {
                //填充产品中feature信息
                detail.withFeatures(t2.getT2());
                //填充注册中心里的产品信息
                return detail.with(t2.getT1());
            })
            .then(Mono.zip(
                //设备信息
                registry
                    .getDevice(device.getId())
                    //先刷新配置缓存
                    .flatMap(operator -> operator.refreshAllConfig().thenReturn(operator))
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
                    .collectList()
            ))
            //填充详情信息
            .flatMap(tp2 -> detail
                .with(tp2.getT1(), tp2.getT2()))
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
                log.warn("get device detail error:{}", err.getLocalizedMessage(), err);
                return Mono.just(detail);
            });
    }

    public Mono<DeviceDetail> getDeviceDetail(String deviceId) {

        return this
            .findById(deviceId)
            .map(Collections::singletonList)
            .flatMapMany(list -> convertDeviceInstanceToDetail(list, true, true, true, true))
            .next();
    }

    public Mono<DeviceState> getDeviceState(String deviceId) {
        return registry.getDevice(deviceId)
                       .flatMap(DeviceOperator::checkState)
                       .flatMap(state -> {
                           DeviceState deviceState = DeviceState.of(state);
                           return createUpdate()
                               .set(DeviceInstanceEntity::getState, deviceState)
                               .where(DeviceInstanceEntity::getId, deviceId)
                               .execute()
                               .thenReturn(deviceState);
                       })
                       .defaultIfEmpty(DeviceState.notActive);
    }

    //获取设备属性
    @SneakyThrows
    public Mono<Map<String, Object>> readProperty(String deviceId,
                                                  String property) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(handleDeviceNotFoundInRegistry(deviceId))
            .map(DeviceOperator::messageSender)//发送消息到设备
            .map(sender -> sender.readProperty(property).messageId(IDGenerator.SNOW_FLAKE_STRING.generate()))
            .flatMapMany(ReadPropertyMessageSender::send)
            .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
            .reduceWith(LinkedHashMap::new, (main, map) -> {
                main.putAll(map);
                return main;
            });

    }

    //获取标准设备属性
    @SneakyThrows
    public Mono<DeviceProperty> readAndConvertProperty(String deviceId,
                                                       String property) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(handleDeviceNotFoundInRegistry(deviceId))
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
                        .map(deviceMetadata -> DeviceProperty.of(value, deviceMetadata.getPropertyOrNull(property)));
                }));

    }

    //设置设备属性
    @SneakyThrows
    public Mono<Map<String, Object>> writeProperties(String deviceId,
                                                     Map<String, Object> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(handleDeviceNotFoundInRegistry(deviceId))
            .flatMap(operator -> operator
                .messageSender()
                .writeProperty()
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .write(properties)
                .validate()
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
        return invokeFunction(deviceId, functionId, properties, true);
    }

    //设备功能调用
    @SneakyThrows
    public Flux<?> invokeFunction(String deviceId,
                                  String functionId,
                                  Map<String, Object> properties,
                                  boolean convertReply) {
        return registry
            .getDevice(deviceId)
            .switchIfEmpty(handleDeviceNotFoundInRegistry(deviceId))
            .flatMap(operator -> operator
                .messageSender()
                .invokeFunction(functionId)
                .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                .setParameter(properties)
                .validate()
            )
            .flatMapMany(FunctionInvokeMessageSender::send)
            .flatMap(convertReply ? mapReply(FunctionInvokeMessageReply::getOutput) : Mono::just);


    }

    //获取设备所有属性
    @SneakyThrows
    public Mono<Map<String, Object>> readProperties(String deviceId, List<String> properties) {

        return registry
            .getDevice(deviceId)
            .switchIfEmpty(handleDeviceNotFoundInRegistry(deviceId))
            .map(DeviceOperator::messageSender)
            .flatMapMany((sender) -> sender.readProperty()
                                           .read(properties)
                                           .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                                           .send())
            .flatMap(mapReply(ReadPropertyMessageReply::getProperties))
            .reduceWith(LinkedHashMap::new, (main, map) -> {
                main.putAll(map);
                return main;
            });
    }

    private Flux<Tuple2<DeviceSaveDetail, DeviceInstanceEntity>> prepareBatchSave(Flux<Tuple2<DeviceSaveDetail, DeviceInstanceEntity>> deviceStream) {
        return deviceStream
            .doOnNext(device -> {
                if (!StringUtils.hasText(device.getT2().getProductId())) {
                    throw new IllegalArgumentException("error.productId_cannot_be_empty");
                }
            })
            .collect(Collectors.groupingBy(tp2 -> tp2.getT2().getProductId()))
            .flatMapMany(deviceMap -> {
                Set<String> productId = deviceMap.keySet();
                return deviceProductService
                    .findById(productId)
                    .doOnNext(product -> deviceMap
                        .getOrDefault(product.getId(), Collections.emptyList())
                        .forEach(e -> e.getT2().setProductName(product.getName())))
                    .thenMany(
                        Flux.fromIterable(deviceMap.values())
                            .flatMap(Flux::fromIterable)
                    );
            })
            .doOnNext(device -> {
                device.getT1().prepare();
                if (!StringUtils.hasText(device.getT2().getProductName())) {
                    throw new BusinessException("error.product_does_not_exist", 500, device.getT2().getProductId());
                }
            });

    }

    public Mono<Integer> batchSave(Flux<DeviceSaveDetail> deviceStream) {
        return this
            .prepareBatchSave(deviceStream.map(detail -> Tuples.of(detail, detail.toInstance())))
            .collectList()
            .flatMap(list -> Flux
                .fromIterable(list)
                .map(Tuple2::getT1)
                .flatMapIterable(DeviceSaveDetail::getTags)
                .as(tagRepository::save)
                .then(Flux.fromIterable(list)
                          .map(Tuple2::getT2)
                          .as(this::save))
            ).map(SaveResult::getTotal);
    }

    private static <R extends DeviceMessageReply, T> Function<R, Mono<T>> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                if (StringUtils.isEmpty(reply.getMessage())) {
                    throw new BusinessException("error.reply_is_error");
                }
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return Mono.justOrEmpty(function.apply(reply));
        };
    }

    /**
     * 批量同步设备状态
     *
     * @param batch 设备状态ID流
     * @param force 是否强制获取设备状态,强制获取会去设备连接到服务器检查设备是否真实在线
     * @return 同步数量
     */
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


    public Mono<Void> mergeMetadata(String deviceId, DeviceMetadata metadata, MergeOption... options) {

        return Mono
            .zip(this.findById(deviceId)
                     .flatMap(device -> {
                         if (StringUtils.hasText(device.getDeriveMetadata())) {
                             return Mono.just(device.getDeriveMetadata());
                         } else {
                             return deviceProductService
                                 .findById(device.getProductId())
                                 .map(DeviceProductEntity::getMetadata);
                         }
                     })
                     .flatMap(JetLinksDeviceMetadataCodec.getInstance()::decode),
                 Mono.just(metadata),
                 (older, newer) -> older.merge(newer, options)
            )
            .flatMap(JetLinksDeviceMetadataCodec.getInstance()::encode)
            .flatMap(newMetadata -> createUpdate()
                .set(DeviceInstanceEntity::getDeriveMetadata, newMetadata)
                .where(DeviceInstanceEntity::getId, deviceId)
                .execute()
                .then(
                    registry
                        .getDevice(deviceId)
                        .flatMap(device -> device.updateMetadata(newMetadata))
                ))
            .then();
    }

    public Flux<DeviceTagEntity> queryDeviceTag(String deviceId, String... tags) {
        return tagRepository
            .createQuery()
            .where(DeviceTagEntity::getDeviceId, deviceId)
            .when(tags.length > 0, q -> q.in(DeviceTagEntity::getKey, Arrays.asList(tags)))
            .fetch();
    }

    //删除设备时，删除设备标签
    @EventListener
    public void handleDeviceDelete(EntityDeletedEvent<DeviceInstanceEntity> event) {
        event.async(
            Flux.concat(
                Flux
                    .fromIterable(event.getEntity())
                    .flatMap(device -> registry
                        .unregisterDevice(device.getId())
                        .onErrorResume(err -> Mono.empty())
                    )
                    .then(),
                tagRepository
                    .createDelete()
                    .where()
                    .in(DeviceTagEntity::getDeviceId, event
                        .getEntity()
                        .stream()
                        .map(DeviceInstanceEntity::getId)
                        .collect(Collectors.toSet()))
                    .execute()
            )
        );
    }

    @Override
    public Mono<Integer> insert(DeviceInstanceEntity data) {
        return this
            .handleCreateBefore(data)
            .flatMap(super::insert);
    }

    @Override
    public Mono<Integer> insert(Publisher<DeviceInstanceEntity> entityPublisher) {
        return super
            .insert(Flux.from(entityPublisher)
                        .flatMap(
                            this::handleCreateBefore,
                            16,
                            16));
    }

    @Override
    public Mono<Integer> insertBatch(Publisher<? extends Collection<DeviceInstanceEntity>> entityPublisher) {
        return Flux.from(entityPublisher)
                   .flatMapIterable(Function.identity())
                   .as(this::insert);
    }

    private Mono<DeviceInstanceEntity> handleCreateBefore(DeviceInstanceEntity instanceEntity) {
        return Mono
            .zip(
                deviceProductService.concurrentFindById(instanceEntity.getProductId()),
                registry
                    .getProduct(instanceEntity.getProductId())
                    .flatMap(DeviceProductOperator::getProtocol),
                (product, protocol) -> protocol.doBeforeDeviceCreate(
                    Transport.of(product.getTransportProtocol()),
                    instanceEntity.toDeviceInfo(false))
            )
            .flatMap(Function.identity())
            .doOnNext(info -> {
                if (ObjectUtils.isEmpty(instanceEntity.getId())) {
                    instanceEntity.setId(info.getId());
                }
                instanceEntity.mergeConfiguration(info.getConfiguration());
            })
            .thenReturn(instanceEntity);

    }

    private final CyclicDependencyChecker<DeviceInstanceEntity, Void> checker = CyclicDependencyChecker
        .of(DeviceInstanceEntity::getId, DeviceInstanceEntity::getParentId, this::findById);

    public Mono<Void> checkCyclicDependency(DeviceInstanceEntity device) {
        return checker.check(device);
    }

    public Mono<Void> checkCyclicDependency(String id, String parentId) {
        DeviceInstanceEntity instance = DeviceInstanceEntity.of();
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

    public Mono<? extends DeviceOperator> handleDeviceNotFoundInRegistry(String deviceId) {
        return Mono
            .defer(() -> findById(deviceId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException.NoStackTrace("error.device_not_found", deviceId)))
                .flatMap(ignore -> Mono.error(() -> new NotFoundException.NoStackTrace("error.device_not_activated", deviceId)))
            );
    }

    public Mono<Void> handleUnbind(String gatewayId, List<String> deviceIdList) {
        return this
            .createUpdate()
            .setNull(DeviceInstanceEntity::getParentId)
            .in(DeviceInstanceEntity::getId, deviceIdList)
            .and(DeviceInstanceEntity::getParentId, gatewayId)
            .execute()
            .then(handleBindUnbind(gatewayId, Flux.fromIterable(deviceIdList), ProtocolSupport::onChildUnbind));
    }


    private Mono<Void> handleBindUnbind(String gatewayId,
                                        Flux<String> childId,
                                        Function3<ProtocolSupport, DeviceOperator, Flux<DeviceOperator>, Mono<Void>> operator) {
        return registry
            .getDevice(gatewayId)
            .flatMap(gateway -> gateway
                .getProtocol()
                .flatMap(protocol -> operator.apply(protocol, gateway, childId.flatMap(registry::getDevice)))
            );
    }

    public Mono<DeviceMetadata> getMetadata(String id) {
        return registry
            .getDevice(id)
            .flatMap(DeviceOperator::getMetadata)
            .switchIfEmpty(
                findById(id)
                    .zipWhen(
                        device -> deviceProductService.getMetadata(device.getProductId()),
                        (device, product) -> {
                            if (StringUtils.hasText(device.getDeriveMetadata())) {
                                return new CompositeDeviceMetadata(
                                    JetLinksDeviceMetadataCodec.getInstance().doDecode(device.getDeriveMetadata()),
                                    product);
                            }
                            return product;
                        })
            );
    }
}
