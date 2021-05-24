package org.jetlinks.community.device.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.enums.DeviceFeature;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.*;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceMessageBusinessHandler {

    private final LocalDeviceInstanceService deviceService;

    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final EventBus eventBus;

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
                DeviceInstanceEntity instance = new DeviceInstanceEntity();
                instance.setId(tps.getT1());
                instance.setName(tps.getT2());
                instance.setProductId(tps.getT3());
                instance.setProductName(tps.getT4().getName());
                instance.setConfiguration(tps.getT5());
                instance.setRegistryTime(message.getTimestamp());
                instance.setCreateTimeNow();
                instance.setCreatorId(tps.getT4().getCreatorId());
                instance.setOrgId(tps.getT4().getOrgId());
                @SuppressWarnings("all")
                boolean selfManageState = CastUtils
                    .castBoolean(tps.getT5().getOrDefault(DeviceConfigKey.selfManageState.getKey(), false));

                if (selfManageState) {
                    instance.addFeature(DeviceFeature.selfManageState);
                }

                instance.setState(selfManageState ? DeviceState.offline : DeviceState.online);

                return deviceService
                    .save(Mono.just(instance))
                    .thenReturn(instance)
                    .flatMap(device -> registry
                        .register(device.toDeviceInfo()
                                        .addConfig("state", selfManageState
                                            ? org.jetlinks.core.device.DeviceState.offline
                                            : org.jetlinks.core.device.DeviceState.online)));
            });
    }

    @Subscribe("/device/*/*/register")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> autoRegisterDevice(DeviceRegisterMessage message) {
        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> {
                @SuppressWarnings("all")
                Map<String, Object> config = message.getHeader("configuration").map(Map.class::cast).orElse(null);
                if (MapUtils.isNotEmpty(config)) {
                    return device
                        .setConfigs(config)
                        .thenReturn(device);
                }
                return Mono.just(device);
            })
            .switchIfEmpty(Mono.defer(() -> {
                //自动注册
                return doAutoRegister(message);
            }))
            .then();
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
                .flatMap(dev -> dev
                    .setConfig(DeviceConfigKey.parentGatewayId, message.getDeviceId())
                    .thenReturn(dev))
                .flatMap(DeviceOperator::getState)
                .flatMap(state -> deviceService
                    .createUpdate()
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

            return registry
                .getDevice(childId)
                .flatMap(dev -> dev
                    .removeConfig(DeviceConfigKey.parentGatewayId.getKey())
                    .then(dev.checkState()))
                .flatMap(state -> deviceService
                    .createUpdate()
                    .setNull(DeviceInstanceEntity::getParentId)
                    .set(DeviceInstanceEntity::getState, DeviceState.of(state))
                    .where(DeviceInstanceEntity::getId, childId)
                    .execute()
                    .then());
        }
        return Mono.empty();
    }

    @Subscribe("/device/*/*/unregister")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> unRegisterDevice(DeviceUnRegisterMessage message) {
        //注销设备
        return deviceService
            .unregisterDevice(message.getDeviceId())
            .then();
    }

    @Subscribe("/device/*/*/message/tags/update")
    public Mono<Void> updateDeviceTag(UpdateTagMessage message) {
        Map<String, Object> tags = message.getTags();
        String deviceId = message.getDeviceId();

        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> Flux
                .fromIterable(tags.entrySet())
                .map(e -> {
                    DeviceTagEntity tagEntity = metadata
                        .getTag(e.getKey())
                        .map(tagMeta -> DeviceTagEntity.of(tagMeta, e.getValue()))
                        .orElseGet(() -> {
                            DeviceTagEntity entity = new DeviceTagEntity();
                            entity.setKey(e.getKey());
                            entity.setType("string");
                            entity.setName(e.getKey());
                            entity.setCreateTime(new Date());
                            entity.setDescription("设备上报");
                            entity.setValue(String.valueOf(e.getValue()));
                            return entity;
                        });
                    tagEntity.setDeviceId(deviceId);
                    tagEntity.setId(DeviceTagEntity.createTagId(deviceId, tagEntity.getKey()));
                    return tagEntity;
                }))
            .as(tagRepository::save)
            .then();
    }

    @Subscribe("/device/*/*/metadata/derived")
    public Mono<Void> updateMetadata(DerivedMetadataMessage message) {
        if (message.isAll()) {
            return updateMedata(message.getDeviceId(), message.getMetadata());
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
            .flatMap(metadata -> updateMedata(message.getDeviceId(), metadata));
    }

    private Mono<Void> updateMedata(String deviceId, String metadata) {
        return deviceService
            .createUpdate()
            .set(DeviceInstanceEntity::getDeriveMetadata, metadata)
            .where(DeviceInstanceEntity::getId, deviceId)
            .execute()
            .then(registry.getDevice(deviceId))
            .flatMap(device -> device.updateMetadata(metadata))
            .then();
    }


    @PostConstruct
    public void init() {

        Subscription subscription = Subscription
            .builder()
            .subscriberId("device-state-synchronizer")
            .topics("/device/*/*/online", "/device/*/*/offline")
            .justLocal()//只订阅本地
            .build();

        //订阅设备上下线消息,同步数据库中的设备状态,
        //最小间隔800毫秒,最大缓冲数量500,最长间隔2秒.
        //如果2条消息间隔大于0.8秒则不缓冲直接更新
        //否则缓冲,数量超过500后批量更新
        //无论缓冲区是否超过500条,都每2秒更新一次.
        FluxUtils.bufferRate(eventBus
                                 .subscribe(subscription, DeviceMessage.class)
                                 .map(DeviceMessage::getDeviceId),
                             800, Integer.getInteger("device.state.sync.batch", 500), Duration.ofSeconds(2))
                 .onBackpressureBuffer(64,
                                       list -> log.warn("无法处理更多设备状态同步!"),
                                       BufferOverflowStrategy.DROP_OLDEST)
                 .publishOn(Schedulers.boundedElastic(), 64)
                 .concatMap(list -> deviceService.syncStateBatch(Flux.just(list), false).map(List::size))
                 .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
                 .subscribe((i) -> log.info("同步设备状态成功:{}", i));

    }

}
