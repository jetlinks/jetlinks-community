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
package org.jetlinks.community.device.service.tag;

import lombok.*;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.message.UpdateTagMessage;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.things.data.ThingsDataWriter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeviceTagSynchronizer implements CommandLineRunner {

    private final DeviceTagProperties properties;

    private final DeviceRegistry registry;

    private final ThingsDataWriter dataWriter;

    private final ThingsDataManager dataManager;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    public PersistenceBuffer<DeviceTagBuffer> buffer;

    @Subscribe(value = "/device/*/*/message/tags/update")
    public Mono<Void> updateDeviceTag(UpdateTagMessage message) {
        Map<String, Object> tags = message.getTags();
        if (MapUtils.isEmpty(tags)) {
            return Mono.empty();
        }
        String deviceId = message.getDeviceId();

        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> Flux
                .fromIterable(tags.entrySet())
                .filter(e -> e.getValue() != null)
                .flatMap(e -> {
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
                    tagEntity.setTimestamp(message.getTimestamp());
                    tagEntity.setDeviceId(deviceId);
                    tagEntity.setId(DeviceTagEntity.createTagId(deviceId, tagEntity.getKey()));

                    return dataWriter
                        .updateTag(DeviceThingType.device.getId(),
                                   tagEntity.getDeviceId(),
                                   tagEntity.getKey(),
                                   System.currentTimeMillis(),
                                   e.getValue())
                        .then(writeBuffer(tagEntity));

                }))
            .then();
    }


    public Mono<Void> writeBuffer(DeviceTagEntity entity) {
        return buffer.writeAsync(new DeviceTagBuffer(entity));
    }


    private Mono<DeviceTagEntity> convertEntity(DeviceTagBuffer buffer) {
        //从最新缓存中获取最新的数据,并填入准备入库的实体中
        return dataManager
            .getLastTag(DeviceThingType.device.getId(),
                        buffer.getTag().getDeviceId(),
                        buffer.getTag().getKey(),
                        System.currentTimeMillis())
            .map(tag -> {
                //缓存中的数据比buffer中的新,则更新为buffer中的数据
                if (tag.getTimestamp() >= buffer.tag.getTimestamp()) {
                    buffer.getTag().setTimestamp(tag.getTimestamp());
                    buffer.getTag().setValue(String.valueOf(tag.getValue()));
                }
                return buffer.getTag();
            })
            .defaultIfEmpty(buffer.tag);
    }

    public Mono<Boolean> handleBuffer(Flux<DeviceTagBuffer> buffer) {

        return tagRepository
            .save(buffer.flatMap(this::convertEntity))
            .contextWrite(ctx -> ctx.put(DeviceTagSynchronizer.class, this))
            .then(Reactors.ALWAYS_FALSE);
    }

    @EventListener
    public void handleDeviceTagEvent(EntityCreatedEvent<DeviceTagEntity> event) {
        event.async(updateTag(event.getEntity()));
    }

    @EventListener
    public void handleDeviceTagEvent(EntitySavedEvent<DeviceTagEntity> event) {
        event.async(updateTag(event.getEntity()));
    }

    @EventListener
    public void handleDeviceTagEvent(EntityModifyEvent<DeviceTagEntity> event) {
        event.async(updateTag(event.getAfter()));
    }

    @EventListener
    public void handleDeviceTagEvent(EntityDeletedEvent<DeviceTagEntity> event) {
        event.async(
            Flux
                .fromIterable(event.getEntity())
                .flatMap(entity -> dataWriter
                    .removeTag(DeviceThingType.device.getId(),
                               entity.getDeviceId(),
                               entity.getKey())
                    .then()
                ));
    }


    /**
     * 更新标签,界面上手动修改标签?
     *
     * @param entityList 标签
     * @return Void
     */
    private Mono<Void> updateTag(List<DeviceTagEntity> entityList) {
        return Mono.deferContextual(ctx -> {
            //更新来自消息的标签,不需要再次更新
            if (ctx.hasKey(DeviceTagSynchronizer.class)) {
                return Mono.empty();
            }
            return Flux
                .fromIterable(entityList)
                .flatMap(entity -> dataWriter
                    .updateTag(DeviceThingType.device.getId(),
                               entity.getDeviceId(),
                               entity.getKey(),
                               System.currentTimeMillis(),
                               entity.parseValue()))
                .then();
        });
    }

    @PostConstruct
    public void init() {
        buffer = new PersistenceBuffer<>(
            BufferSettings.create(properties),
            DeviceTagBuffer::new,
            this::handleBuffer)
            .name("device-tag-synchronizer");
        buffer.init();
    }

    @PreDestroy
    public void shutdown() {
        buffer.stop();
    }

    @Override
    public void run(String... args) throws Exception {
        buffer.start();
        SpringApplication
            .getShutdownHandlers()
            .add(buffer::dispose);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceTagBuffer implements Externalizable {
        private DeviceTagEntity tag;

        @Override
        public void writeExternal(ObjectOutput out) {
            tag.writeExternal(out);
        }

        @Override
        public void readExternal(ObjectInput in) {
            tag = new DeviceTagEntity();
            tag.readExternal(in);
        }
    }
}
