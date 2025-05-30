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
package org.jetlinks.community.protocol.manager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.protocol.ProtocolSupportEntity;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.supports.protocol.StaticProtocolSupports;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Slf4j
public class LocalProtocolSupportManager
    extends StaticProtocolSupports implements CommandLineRunner {


    private final ProtocolSupportLoader loader;

    private final DataReferenceManager referenceManager;

    private final ReactiveRepository<ProtocolSupportEntity, String> repository;

    private final Duration loadProtocolTimeout =
        Duration.ofSeconds(Integer.getInteger("jetlinks.device.protocol.load.timeout", 30));


    public LocalProtocolSupportManager(DataReferenceManager referenceManager,
                                       ProtocolSupportLoader loader,
                                       ReactiveRepository<ProtocolSupportEntity, String> repository) {
        this.loader = loader;
        this.referenceManager = referenceManager;
        this.repository = repository;
    }

    //删除协议前,判断协议是否已被引用。已被引用的协不能删除
    @EventListener
    public void handleProtocolDelete(EntityBeforeDeleteEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> referenceManager
                    .assertNotReferenced(DataReferenceManager.TYPE_PROTOCOL, entity.getId(), "error.protocol_referenced"))
        );
    }

    //保存协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntityBeforeSaveEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> checkProtocol(entity.toDefinition()))
        );
    }

    //创建协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntityBeforeCreateEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> checkProtocol(entity.toDefinition()))
        );
    }


    //修改协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntityBeforeModifyEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(entity -> checkProtocol(entity.toDefinition()))
        );
    }

    //删除协议前,判断协议是否已被引用。已被引用的协不能删除
    @EventListener
    public void handleProtocolDelete(EntityDeletedEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .doOnNext(entity -> remove(entity.toDefinition()))
        );
    }

    //保存协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntitySavedEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> init(entity.toDefinition()))
        );
    }

    //创建协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntityCreatedEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> init(entity.toDefinition()))
        );
    }


    //修改协议前，判断协议是否能正常运行
    @EventListener
    public void checkProtocol(EntityModifyEvent<ProtocolSupportEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(entity -> checkProtocol(entity.toDefinition()))
        );
    }


    public void remove(ProtocolSupportDefinition definition) {
        ProtocolSupport protocol = supports.get(definition.getId());
        if (protocol != null) {
            protocol.dispose();
        }
    }

    public void loadAllProtocol() {
        try {
            repository
                .createQuery()
                .where(ProtocolSupportEntity::getState, 1)
                .fetch()
                .map(ProtocolSupportEntity::toDefinition)
                .flatMap(def -> this
                    .init(def)
                    .onErrorResume(err -> Mono.empty()))
                .blockLast(loadProtocolTimeout);
        } catch (Throwable error) {
            log.warn("load protocol error", error);
        }
    }


    public void init() {
        loadAllProtocol();
    }


    public Mono<Void> init(ProtocolSupportDefinition definition) {

        return doProtocolInit(definition);
    }

    protected void loadError(ProtocolSupportDefinition def, Throwable err) {

    }

    protected ProtocolSupport afterLoaded(ProtocolSupportDefinition def, ProtocolSupport support) {

        return support;
    }


    @Override
    public void run(String... args) {
        init();
    }


    /**
     * 初始化协议
     * <p>
     * 如果{@link ProtocolSupportDefinition#getState()}不为1，则表示卸载协议。为0则表示加载协议.
     *
     * @param definition 协议定义
     */
    protected Mono<Void> doProtocolInit(ProtocolSupportDefinition definition) {
        return Mono
            .defer(() -> {
                String operation = definition.getState() != 1 ? "uninstall" : "install";
                try {
                    if (definition.getState() != 1) {
                        ProtocolSupport protocol = supports.get(definition.getId());
                        if (protocol != null) {
                            log.debug("uninstall protocol:{}", definition);
                            unRegister(protocol);
                            return Mono.empty();
                        }
                    }
                    Consumer<ProtocolSupport> consumer = definition.getState() != 1 ? this::unRegister : this::register;

                    log.debug("{} protocol:{}", operation, definition);

                    return loader
                        .load(definition)
                        .doOnNext(e -> {
                            e.init(definition.getConfiguration());
                            log.debug("{} protocol[{}] success: {}", operation, definition.getId(), e);
                            consumer.accept(afterLoaded(definition, e));
                        })
                        .onErrorResume((e) -> {
                            log.error("{} protocol[{}] error", operation, definition.getId(), e);
                            loadError(definition, e);
                            return Mono.empty();
                        })
                        .then();
                } catch (Throwable err) {
                    log.error("{} protocol error", operation, err);
                    loadError(definition, err);
                }
                return Mono.empty();
            })
            .as(MonoTracer.create("/protocol/" + definition.getId() + "/init"));
    }


    // 协议检查
    protected Mono<Void> checkProtocol(ProtocolSupportDefinition definition) {
        if (definition == null || definition.getState() != 1) {
            return Mono.empty();
        }
        return loader
            //加载一下检验是否正确，然后就卸载
            .load(definition)
            .doOnNext(ProtocolSupport::dispose)
            .thenReturn(definition)
            .as(LocaleUtils::transform)
            .onErrorMap(err -> {
                BusinessException e = new BusinessException("error.unable_to_load_protocol", 500, err.getLocalizedMessage());
                e.addSuppressed(err);
                return e;
            })
            .then();
    }

}
