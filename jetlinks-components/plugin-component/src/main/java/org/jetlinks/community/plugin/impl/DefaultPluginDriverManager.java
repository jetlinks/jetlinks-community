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
package org.jetlinks.community.plugin.impl;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.plugin.PluginDriverInstaller;
import org.jetlinks.community.plugin.PluginDriverListener;
import org.jetlinks.community.plugin.PluginDriverManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 使用{@link PluginDriverEntity}来管理插件驱动，在对{@link PluginDriverEntity}CRUD时,会自动进行安装和卸载操作
 *
 * @author zhouhao
 * @since 2.0
 */
@Slf4j
public class DefaultPluginDriverManager implements PluginDriverManager, CommandLineRunner {

    private final ReactiveCacheContainer<String, PluginDriver> cache = ReactiveCacheContainer.create();

    private final PluginDriverInstaller installer;

    private final ReactiveRepository<PluginDriverEntity, String> repository;

    private final List<PluginDriverListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultPluginDriverManager(ReactiveRepository<PluginDriverEntity, String> repository,
                                      PluginDriverInstaller installer) {
        this.installer = installer;
        this.repository = repository;
    }


    @Override
    public Flux<PluginDriver> getDrivers() {
        return cache.values();
    }

    @Override
    public Mono<PluginDriver> getDriver(String id) {
        PluginDriver cached = cache.getNow(id);
        if (null != cached) {
            return Mono.just(cached);
        }

        AtomicBoolean installed = new AtomicBoolean();
        return cache
            .computeIfAbsent(id, _id -> repository
                .findById(_id)
                .flatMap(e -> {
                    installed.set(true);
                    return installer.install(e.toConfig());
                }))
            //加载完成后再执行监听器
            .flatMap(driver -> {
                if (installed.get()) {
                    return this
                        .fireListener(listener -> listener.onInstall(id, driver))
                        .thenReturn(driver);
                }
                return Mono.just(driver);
            });
    }

    @Override
    public Disposable listen(PluginDriverListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private Mono<Void> fireListener(Function<PluginDriverListener, Mono<Void>> call) {
        if (listeners.isEmpty()) {
            return Mono.empty();
        }
        return Flux
            .fromIterable(listeners)
            .flatMap(listener -> call
                .apply(listener)
                .onErrorResume(err -> {
                    log.warn("fire driver listener[{}] error", listener, err);
                    return Mono.empty();
                }))
            .then();
    }

    private Mono<PluginDriver> loadDriver(PluginDriverEntity entity){
        return loadDriver(entity,true);
    }

    private Mono<PluginDriver> loadDriver(PluginDriverEntity entity,boolean reload) {
        PluginDriver[] _old = new PluginDriver[1];
        return cache
            .compute(entity.getId(), (key, old) -> {
                _old[0] = old;
                if (old == null) {
                    return installer
                        .install(entity.toConfig())
                        .map(PluginDriverWrapper::new);
                }
                if(reload){
                    return installer
                        .reload(old.unwrap(PluginDriver.class), entity.toConfig())
                        .map(PluginDriverWrapper::new);
                }
                return Mono.just(old);
            })
            //加载完成后再触发监听器,否则如果在监听器中获取驱动可能会导致"死锁"
            .flatMap(driver -> {
                if (reload && _old[0] != null) {
                    return this
                        .fireListener(listener -> listener.onReload(entity.getId(), _old[0], driver))
                        .thenReturn(driver);
                }
                if (_old[0] == null) {
                    return this
                        .fireListener(listener -> listener.onInstall(entity.getId(), driver))
                        .thenReturn(driver);
                }
                return Mono.just(driver);
            })
            .onErrorMap(err -> new BusinessException.NoStackTrace("error.unable_to_load_plugin_driver", err));
    }

    @EventListener
    public void handleEvent(EntityCreatedEvent<PluginDriverEntity> event) {
          event.async(
             Flux.fromIterable(event.getEntity())
                 .concatMap(this::loadDriver)
          );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<PluginDriverEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .concatMap(this::loadDriver)
        );
    }
    @EventListener
    public void handleEvent(EntityModifyEvent<PluginDriverEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .concatMap(this::loadDriver)
        );
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<PluginDriverEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .concatMap(this::doHandleDeleted)
        );
    }

    public Mono<Void> doHandleDeleted(PluginDriverEntity entity) {
        PluginDriver driver = cache.remove(entity.getId());
        if (null != driver) {
            return this
                .fireListener(listener -> listener.onUninstall(entity.getId(), driver))
                .then(installer.uninstall(entity.toConfig()))
                .onErrorResume(err -> {
                    log.warn("uninstall driver [{}] error", entity.getId(), err);
                    return Mono.empty();
                });
        }
        return installer
            .uninstall(entity.toConfig())
            .onErrorResume(err -> {
                log.warn("uninstall driver [{}] error", entity.getId(), err);
                return Mono.empty();
            });
    }

    @Override
    public void run(String... args) throws Exception {
        repository
            .createQuery()
            .fetch()
            .flatMap(e -> this
                .loadDriver(e, false)
                .onErrorResume(err -> {
                    log.error("loader plugin driver [{}] error", e.getId(), err);
                    return Mono.empty();
                }))
            .then(fireListener(PluginDriverListener::onStartup))
            .subscribe();
    }
}
