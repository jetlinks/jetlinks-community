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
package org.jetlinks.community.network.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.core.ProtocolSupport;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.jetlinks.community.network.manager.enums.DeviceGatewayState.enabled;

/**
 * @author wangzheng
 * @since 1.0
 */
@Order(1)
@Component
@Slf4j
public class DeviceGatewayEventHandler implements CommandLineRunner {

    public static final String DO_NOT_RELOAD_GATEWAY = "_do_not_reload_device_gateway";

    private final DeviceGatewayService deviceGatewayService;

    private final DeviceGatewayManager deviceGatewayManager;

    private final DataReferenceManager referenceManager;

    private final Duration gatewayStartupDelay = Duration.ofSeconds(5);

    public DeviceGatewayEventHandler(DeviceGatewayService deviceGatewayService,
                                     DeviceGatewayManager deviceGatewayManager,
                                     DataReferenceManager referenceManager) {
        this.deviceGatewayService = deviceGatewayService;
        this.deviceGatewayManager = deviceGatewayManager;
        this.referenceManager = referenceManager;
    }

    @EventListener
    public void handlePrepareSave(EntityPrepareSaveEvent<DeviceGatewayEntity> event) {
        putGatewayInfo(event.getEntity());
        event.async(gatewayConfigValidate(event.getEntity()));
    }

    @EventListener
    public void handlePrepareCreate(EntityPrepareCreateEvent<DeviceGatewayEntity> event) {
        putGatewayInfo(event.getEntity());
        event.async(gatewayConfigValidate(event.getEntity()));
    }

    @EventListener
    public void handlePrepareUpdate(EntityPrepareModifyEvent<DeviceGatewayEntity> event) {
        putGatewayInfo(event.getAfter());
        event.async(gatewayConfigValidate(event.getAfter()));
    }

    @EventListener
    public void handleGatewayDelete(EntityBeforeDeleteEvent<DeviceGatewayEntity> event) {
        //删除网关时检测是否已被使用
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(gateway -> referenceManager
                    .assertNotReferenced(DataReferenceManager.TYPE_DEVICE_GATEWAY, gateway.getId(), "error.device_gateway_referenced"))
        );
    }

    @EventListener
    public void handleCreated(EntityCreatedEvent<DeviceGatewayEntity> event) {
        event.async(
            reloadGateway(Flux
                              .fromIterable(event.getEntity())
                              .filter(gateway -> gateway.getState() == enabled))
        );
    }

    @EventListener
    public void handleSaved(EntitySavedEvent<DeviceGatewayEntity> event) {
        event.async(
            reloadGateway(Flux
                              .fromIterable(event.getEntity())
                              .filter(gateway -> gateway.getState() == enabled))
        );
    }

    //网关更新时，自动重新加载
    @EventListener
    public void handleModify(EntityModifyEvent<DeviceGatewayEntity> event) {
        event.async(
            Mono.deferContextual(ctx -> {
                if (ctx.getOrEmpty(DO_NOT_RELOAD_GATEWAY).isPresent()) {
                    return Mono.empty();
                }
                return reloadGateway(Flux
                                         .fromIterable(event.getAfter())
                                         .filter(gateway -> gateway.getState() == enabled));
            })
        );
    }

    private Mono<Void> reloadGateway(Flux<org.jetlinks.community.network.manager.entity.DeviceGatewayEntity> gatewayEntities) {
        return gatewayEntities
            .flatMap(gateway -> deviceGatewayManager.reload(gateway.getId()))
            .then();
    }

    private void putGatewayInfo(List<DeviceGatewayEntity> entities) {
        for (DeviceGatewayEntity entity : entities) {
            DeviceGatewayProvider provider = deviceGatewayManager
                .getProvider(entity.getProvider())
                .orElse(null);
            if (provider == null) {
                continue;
            }
            if (!StringUtils.hasText(entity.getId())) {
                entity.setId(IDGenerator.SNOW_FLAKE_STRING.generate());
            }
            //接入方式
            entity.setChannel(provider.getChannel());

            //传输协议,如TCP,MQTT,ModBus
            if (!StringUtils.hasText(entity.getTransport())) {
                entity.setTransport(provider.getTransport().getId());
            }

            //没有指定channelId则使用id作为channelId
            if (!StringUtils.hasText(entity.getChannelId())) {
                entity.setChannelId(entity.getId());
            }
            //协议
            if (provider instanceof ProtocolSupport) {
                entity.setProtocol(provider.getId());
            }
        }
    }

    // 检验网关配置参数
    private Mono<Void> gatewayConfigValidate(List<DeviceGatewayEntity> entityList) {
        return Flux.fromIterable(entityList)
                   .filter(entity -> entity.getConfiguration() != null && entity.getState() == enabled)
                   .flatMap(entity ->
                                Mono.justOrEmpty(deviceGatewayManager.getProvider(entity.getProvider()))
                                    //当分布式部署时,每个服务支持的网关可能不同.
//                                    .switchIfEmpty(Mono.error(
//                                        () -> new UnsupportedOperationException("error.unsupported_device_gateway_provider")
//                                    ))
                                    .flatMap(gatewayProvider -> gatewayProvider.createDeviceGateway(entity.toProperties())))
                   .then();
    }

    @Override
    public void run(String... args) {
//        log.debug("start device gateway in {} later", gatewayStartupDelay);
//        Mono.delay(gatewayStartupDelay)
//            .then(
//                Mono.defer(() -> deviceGatewayService
//                    .createQuery()
//                    .where()
//                    .and(DeviceGatewayEntity::getState, DeviceGatewayState.enabled)
//                    .fetch()
//                    .map(DeviceGatewayEntity::getId)
//                    .flatMap(id -> Mono
//                        .defer(() -> deviceGatewayManager
//                            .getGateway(id)
//                            .flatMap(DeviceGateway::startup))
//                        .onErrorResume((err) -> {
//                            log.error(err.getMessage(), err);
//                            return Mono.empty();
//                        })
//                    )
//                    .then())
//            )
//            .subscribe();
    }
}
