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

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.events.DeviceAutoRegisterEvent;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 设备消息相关业务逻辑处理类.通过监听事件来进行处理:
 * <pre>
 *
 * 同步设备的在线状态:{@link DeviceMessageBusinessHandler#init()}
 * 处理自动注册:{@link DeviceMessageBusinessHandler#autoRegisterDevice(DeviceRegisterMessage)}
 * 处理子设备注册:{@link DeviceMessageBusinessHandler#autoBindChildrenDevice(ChildDeviceMessage)}
 * 处理子设备注销: {@link DeviceMessageBusinessHandler#autoUnbindChildrenDevice(ChildDeviceMessage)}
 * 处理派生物模型:{@link DeviceMessageBusinessHandler#updateMetadata(DerivedMetadataMessage)}
 *
 * </pre>
 *
 * @author zhouhao
 * @since 1.5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceMessageBusinessHandler implements CommandLineRunner {

    private static final long[] metadataUpdateRetryDelay = new long[]{50, 100, 100, 250};

    private final LocalDeviceInstanceService deviceService;

    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final EventBus eventBus;

    private final ApplicationEventPublisher eventPublisher;

    private final Disposable.Composite disposable = Disposables.composite();


    /**
     * 自动注册设备信息
     * <p>
     * 设备消息的header需要包含{@code deviceName},{@code productId}才会自动注册.、
     * <p>
     * 注册前会推送{@link DeviceAutoRegisterEvent}事件到spring,可以通过监听此事件来处理自定义逻辑,比如某些情况不允许自动注册.
     * <pre>{@code
     *
     * @EventListener
     * public void handleAutoRegister(DeviceAutoRegisterEvent event){
     *
     *      event.async(
     *          this
     *          .checkAllowAutoRegister(event.getEntity())
     *          .doOnNext(event::setAllowRegister)
     *      )
     *
     * }
     *
     * }</pre>
     *
     * @param message 注册消息
     * @return 注册后的设备操作接口
     */
    private Mono<DeviceOperator> doAutoRegister(DeviceRegisterMessage message) {
        //自动注册
        return Mono
            .zip(
                //T1. 设备ID
                Mono.justOrEmpty(message.getDeviceId()),
                //T2. 设备名称
                Mono.justOrEmpty(message.getHeader("deviceName")).map(String::valueOf),
                //T3. 产品ID
                Mono.justOrEmpty(message.getHeader("productId").map(String::valueOf)),
                //T4. 产品
                Mono.justOrEmpty(message.getHeader("productId").map(String::valueOf))
                    .flatMap(productService::findById),
                //T5. 配置信息
                Mono.justOrEmpty(message.getHeader("configuration").map(Map.class::cast).orElse(new HashMap()))
            ).flatMap(tps -> {
                DeviceInstanceEntity instance = DeviceInstanceEntity.of();
                instance.setId(tps.getT1());
                instance.setName(tps.getT2());
                instance.setProductId(tps.getT3());
                instance.setProductName(tps.getT4().getName());
                instance.setConfiguration(tps.getT5());
                instance.setRegistryTime(message.getTimestamp());
                instance.setCreateTimeNow();
                instance.setCreatorId(tps.getT4().getCreatorId());
                //网关ID
                message.getHeader(DeviceConfigKey.parentGatewayId.getKey())
                       .map(String::valueOf)
                       .ifPresent(instance::setParentId);
                if (Objects.equals(instance.getId(), instance.getParentId())) {
                    return Mono.error(new IllegalStateException("设备ID与网关ID不能相同:" + instance.getId()));
                }

                //设备自状态管理
                //网关注册设备子设备时,设置自状态管理。
                //在检查子设备状态时,将会发送ChildDeviceMessage<DeviceStateCheckMessage>到网关
                //网关需要回复ChildDeviceMessageReply<DeviceStateCheckMessageReply>
                @SuppressWarnings("all")
                boolean selfManageState = CastUtils
                    .castBoolean(tps.getT5().getOrDefault(DeviceConfigKey.selfManageState.getKey(), false));
                boolean offline = selfManageState || message.getHeaderOrDefault(Headers.ignoreSession);
                instance.setState(offline ? DeviceState.offline : DeviceState.online);
                //合并配置
                instance.mergeConfiguration(tps.getT5());

                DeviceAutoRegisterEvent event = new DeviceAutoRegisterEvent(instance);
                //循环依赖检查
                event.async(
                    deviceService.checkCyclicDependency(instance)
                );

                return event
                    //先推送DeviceAutoRegisterEvent
                    .publish(eventPublisher)
                    .then(Mono.defer(() -> {
                        //允许注册
                        if (event.isAllowRegister()) {
                            return deviceService
                                .save(instance)
                                .then(Mono.defer(() -> doRegister(instance)));
                        } else {
                            return Mono.empty();
                        }
                    }));
            });
    }

    private Mono<DeviceOperator> doRegister(DeviceInstanceEntity device) {
        return registry
            .register(device
                          .toDeviceInfo()
                          .addConfig("state", device.getState()==DeviceState.online
                              ? org.jetlinks.core.device.DeviceState.online
                              : org.jetlinks.core.device.DeviceState.offline));
    }

    @Subscribe(value = "/device/*/*/register", priority = Integer.MIN_VALUE)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> autoRegisterDevice(DeviceRegisterMessage message) {
        if (message.getHeader(Headers.force).orElse(false)) {
            return this
                .doAutoRegister(message)
                .then();
        }
        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> {
                //注册消息中修改了配置信息
                @SuppressWarnings("all")
                Map<String, Object> config = message.getHeader("configuration").map(Map.class::cast).orElse(null);
                if (MapUtils.isNotEmpty(config)) {

                    return deviceService
                        .mergeConfiguration(device.getDeviceId(), config, update ->
                            //更新设备名称
                            update.set(DeviceInstanceEntity::getName,
                                       message.getHeader(PropertyConstants.deviceName).orElse(null)))
                        .thenReturn(device);
                }
                return Mono.just(device);
            })
            //注册中心中没有此设备则进行自动注册
            .switchIfEmpty(Mono.defer(() -> doAutoRegister(message)))
            .then();
    }


    /**
     * 通过订阅子设备注册消息,自动绑定子设备到网关设备
     *
     * @param message 子设备消息
     * @return void
     */
    @Subscribe(value = "/device/*/*/message/children/*/register", priority = Integer.MIN_VALUE)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> autoBindChildrenDevice(ChildDeviceMessage message) {

        Message childMessage = message.getChildDeviceMessage();
        if (childMessage instanceof DeviceRegisterMessage) {
            String childId = ((DeviceRegisterMessage) childMessage).getDeviceId();
            if (message.getDeviceId().equals(childId)) {
                return Mono.error(new IllegalStateException("设备ID与网关ID不能相同:" + childId));
            }
            //网关设备添加到header中
            childMessage.addHeaderIfAbsent(DeviceConfigKey.parentGatewayId.getKey(), message.getDeviceId());

            return deviceService
                .checkCyclicDependency(childId, message.getDeviceId())
                .then(registry.getDevice(childId))
                .map(device -> device
                    .getState()
                    //更新数据库
                    .flatMap(state -> deviceService
                        .createUpdate()
                        .set(DeviceInstanceEntity::getParentId, message.getDeviceId())
                        //状态还有更好的更新方式?
                        .set(DeviceInstanceEntity::getState, DeviceState.of(state))
                        .where(DeviceInstanceEntity::getId, childId)
                        .execute())
                    //更新缓存
                    .then(device.setConfig(DeviceConfigKey.parentGatewayId, message.getDeviceId()))
                    .thenReturn(device))
                .defaultIfEmpty(Mono.defer(() -> doAutoRegister(((DeviceRegisterMessage) childMessage))))
                .flatMap(Function.identity())
                .then();
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
        Message childMessage = message.getChildDeviceMessage();
        if (childMessage instanceof DeviceUnRegisterMessage) {
            String childId = ((DeviceUnRegisterMessage) childMessage).getDeviceId();
            //解除绑定关系
            return deviceService
                .createUpdate()
                .setNull(DeviceInstanceEntity::getParentId)
                .where(DeviceInstanceEntity::getId, childId)
                .execute()
                .then();
        }
        return Mono.empty();
    }

    @Subscribe("/device/*/*/metadata/derived")
    public Mono<Void> updateMetadata(DerivedMetadataMessage message) {
        if (message.isAll()) {
            return updateMetadata(message.getDeviceId(), message.getMetadata());
        }
        return Mono
            .zip(
                //原始物模型
                registry
                    .getDevice(message.getDeviceId())
                    .flatMap(DeviceOperator::getMetadata),
                //新的物模型
                JetLinksDeviceMetadataCodec
                    .getInstance()
                    .decode(message.getMetadata()),
                //合并在一起
                DeviceMetadata::merge
            )
            //重新编码为字符串
            .flatMap(JetLinksDeviceMetadataCodec.getInstance()::encode)
            //更新物模型
            .flatMap(metadata -> updateMetadata(message.getDeviceId(), metadata));
    }

    /**
     * 支持重试的物模型更新.通过重试来处理并行注册以及物模型更新时可能导致数据库的物模型与缓存中不一致的问题.
     * <p>
     * 如果数据库中或者设备注册中心中没有设备则进行重试.
     * <p>
     * 重试次数以及延迟使用{@link DeviceMessageBusinessHandler#metadataUpdateRetryDelay}定义.
     * <p>
     * 如果返回结果为empty,说明整个重试都没有成功更新物模型.
     *
     * @param deviceId       设备ID
     * @param metadata       物模型
     * @param currentRetries 当前重试次数
     * @return DeviceOperator
     */
    private Mono<DeviceOperator> retryableUpdateMetadata(String deviceId, String metadata, int currentRetries) {
        if (currentRetries >= metadataUpdateRetryDelay.length) {
            return registry.getDevice(deviceId);
        }

        Mono<Long> delay;
        if (currentRetries > 0) {
            delay = Mono.delay(Duration.ofMillis(metadataUpdateRetryDelay[currentRetries]));
            log.info("retry update device [{}] metadata : device not registered", deviceId);
        } else {
            delay = Mono.empty();
        }

        return delay
            .then(this.doUpdateMetadata(deviceId, metadata))
            //设备不存在或者没注册,则重试
            .switchIfEmpty(Mono.defer(() -> retryableUpdateMetadata(deviceId, metadata, currentRetries + 1)));
    }

    private Mono<DeviceOperator> doUpdateMetadata(String deviceId, String metadata) {
        return Mono
            .zip(
                //更新数据库
                deviceService
                    .createUpdate()
                    .set(DeviceInstanceEntity::getDeriveMetadata, metadata)
                    .where(DeviceInstanceEntity::getId, deviceId)
                    .execute()
                    .filter(i -> i >= 1),
                //更新缓存
                registry
                    .getDevice(deviceId)
                    .filterWhen(device -> device.updateMetadata(metadata)),
                (count, device) -> device
            );
    }

    private Mono<Void> updateMetadata(String deviceId, String metadata) {
        //更新数据库中以及注册中心中的物模型数据
        return this
            .retryableUpdateMetadata(deviceId, metadata, 0)
            .doOnNext(device -> log.info("update device [{}] metadata success", device.getDeviceId()))
            .switchIfEmpty(Mono.fromRunnable(() -> log.warn("update device [{}] metadata failed: device not registered", deviceId)))
            .then();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class StateBuf implements Externalizable {
        //有效期一小时
        static long expires = Duration.ofHours(1).toMillis();

        private String id;
        private long time;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(id);
            out.writeLong(time);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            id = in.readUTF();
            time = in.readLong();
        }

        public boolean isEffective() {
            return System.currentTimeMillis() - time < expires;
        }
    }

    private PersistenceBuffer<StateBuf> buffer;

    @PostConstruct
    public void init() {

        Subscription subscription = Subscription
            .builder()
            .subscriberId("device-state-synchronizer")
            .topics("/device/*/*/online", "/device/*/*/offline")
            .justLocal()//只订阅本地
            .build();

        //缓冲同步设备上线信息,在突发大量上下线的情况,减少数据库的压力
        buffer =
            new PersistenceBuffer<>(
                "./data/device-state-buffer",
                "device-state.queue",
                StateBuf::new,
                flux -> deviceService
                    .syncStateBatch(flux
                                        .filter(StateBuf::isEffective)
                                        .map(StateBuf::getId)
                                        .distinct()
                                        .collectList()
                                        .flux(), false)
                    .then(Reactors.ALWAYS_FALSE))
                .name("device-state-synchronizer")
                .parallelism(1)
                .bufferTimeout(Duration.ofSeconds(1))
                .retryWhenError(e -> ErrorUtils
                    .hasException(e,
                                  IOException.class,
                                  QueryTimeoutException.class))
                .bufferSize(1000);

        buffer.init();

        disposable.add(
            eventBus
                .subscribe(subscription, payload -> {
                    DeviceMessage msg = payload.decode(DeviceMessage.class);
                    return buffer.writeAsync(new StateBuf(msg.getDeviceId(), msg.getTimestamp()));
                }));

        disposable.add(buffer);

    }

    @PreDestroy
    public void shutdown() {
        buffer.stop();
    }

    @Override
    public void run(String... args) throws Exception {
        buffer.start();

        //在所有bean之后dispose
        SpringApplication
            .getShutdownHandlers()
            .add(disposable::dispose);
    }
}
