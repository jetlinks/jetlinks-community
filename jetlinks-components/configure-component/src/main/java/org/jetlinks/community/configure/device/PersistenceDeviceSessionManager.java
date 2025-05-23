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
package org.jetlinks.community.configure.device;

import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStoreException;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.StringDataType;
import org.jetlinks.community.configure.cluster.Cluster;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionProvider;
import org.jetlinks.core.server.session.LostDeviceSession;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.community.codec.Serializers;
import org.jetlinks.supports.device.session.ClusterDeviceSessionManager;
import org.jetlinks.supports.utils.MVStoreUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于<a href="https://h2database.com/html/mvstore.html">MvStore</a>的本地磁盘持久化设备会话管理器.
 * <p>
 * 实现{@link PersistentSession}接口的设备会话将持久化到磁盘,在平台重启后,将回复这些设备会话.
 * 可通过实现{@link DeviceSessionProvider}来自定义设备会话的序列化.
 *
 * <p>
 * 如果配置了{@link PersistenceDeviceSessionManager#recordHistory}=true,当设备上线时
 * 将记录设备的历史状态,在平台重启后,将检查这些设备的状态,如果设备离线,将触发离线事件.
 *
 * <p>
 * 优点: 非中心化,不依赖中心化的中间件,集群时,更利于横向拓展。在磁盘性能(SSD)充足的情况下，性能影响很小.
 * <p>
 * 缺点: 依赖磁盘IO,程序异常退出可能导致文件损坏而数据丢失.
 *
 * @author zhouhao
 * @see PersistentSession
 * @see DeviceSessionProvider
 * @see org.jetlinks.core.server.session.DeviceSessionProviders
 * @since 2.0
 */
@Slf4j
public class PersistenceDeviceSessionManager extends ClusterDeviceSessionManager implements CommandLineRunner, ApplicationContextAware {
    protected Supplier<DeviceRegistry> registry;

    private MVMap<String, PersistentSessionData> repository;
    private MVMap<String, Long> history;
    private final Scheduler scheduler = Schedulers.newSingle("device-session-persistence");
    @Getter
    @Setter
    private String filePath;

    @Getter
    @Setter
    //刷新间隔,将session进行持久化的间隔周期.
    private Duration flushInterval = Duration.ofMinutes(10);

    //记录历史上线设备,重启后将对这些设备进行状态检查,如果离线将触发离线事件.
    @Getter
    @Setter
    private boolean recordHistory = true;

    //启动后延迟同步设备状态,默认10秒
    @Getter
    @Setter
    private Duration stateSyncDelay = Duration.ofSeconds(10);

    public PersistenceDeviceSessionManager(RpcManager rpcManager) {
        super(rpcManager);
    }

    void initStore(String file) {

        MVStoreUtils
            .open(
                new File(file),
                "device-session",
                builder -> builder.cacheSize(32),
                store -> {
                    repository =
                        store.openMap("device-session-2", new MVMap
                            .Builder<String, PersistentSessionData>()
                            .keyType(StringDataType.INSTANCE)
                            .valueType(new PersistentSessionDataType()));

                    history = MVStoreUtils.openMap(store, "history-2", new MVMap.Builder<>());

                    //兼容旧数据
                    if (store.getMapNames().contains("device-session")) {
                        restore(
                            MVStoreUtils.openMap(store, "device-session", new MVMap.Builder<>())
                        );
                        store.removeMap("device-session");
                    }

                    return null;
                });

    }

    private synchronized void initRepository() {
        try {
            if (repository != null && !repository.store.isClosed()) {
                repository
                    .store
                    .close(1000);
            }
        } catch (Throwable ignore) {
        }

        initStore(filePath);

    }

    private void restore(MVMap<String, PersistentSessionEntity> repo) {

        for (Map.Entry<String, PersistentSessionEntity> e : repo.entrySet()) {
            byte[] data = Base64.decodeBase64(e.getValue().getSessionBase64());

            repository
                .put(e.getKey(), new PersistentSessionData(
                    e.getValue().getProvider(),
                    e.getValue().getDeviceId(),
                    data));

            repo.remove(e.getKey());
        }
    }

    @Override
    public void init() {
        super.init();
        if (filePath == null) {
            filePath = "./data/sessions/" + (Cluster.safeId());
        }
        initRepository();
        if (!flushInterval.isZero() && !flushInterval.isNegative()) {
            disposable.add(
                Flux
                    .interval(flushInterval, Schedulers.boundedElastic())
                    .onBackpressureDrop()
                    .concatMap(ignore -> Flux
                        .fromIterable(localSessions.values())
                        .mapNotNull(ref -> {
                            if (ref.loaded != null && ref.loaded.isWrapFrom(PersistentSession.class)) {
                                return ref.loaded.unwrap(PersistentSession.class);
                            }
                            return null;
                        })
                        .as(this::tryPersistent), 1)
                    .subscribe()
            );
        }

        disposable.add(
            listenEvent(event -> Mono
                .deferContextual(ctx -> {
                    if (ctx.hasKey(PersistenceDeviceSessionManager.class)) {
                        return Mono.empty();
                    }
                    //持久化
                    if (event.getSession().isWrapFrom(PersistentSession.class)) {
                        if (event.getType() == DeviceSessionEvent.Type.unregister) {
                            return removePersistentSession(event.getSession().unwrap(PersistentSession.class));
                        }
                    } else if (recordHistory) {
                        //记录历史设备
                        if (event.getType() == DeviceSessionEvent.Type.register) {
                            return this
                                .operateInStore(
                                    () -> history,
                                    history -> history.put(event.getSession().getDeviceId(),
                                                           event.getSession().connectTime())
                                )
                                .then();
                        }
                        if (event.getType() == DeviceSessionEvent.Type.unregister && !isShutdown()) {
                            return this
                                .operateInStore(
                                    () -> history,
                                    history -> history.remove(event.getSession().getDeviceId())
                                )
                                .then();
                        }
                    }
                    return Mono.empty();
                }))
        );

        //disposable.add(scheduler);
    }


    private <K, V> Mono<V> operateInStore(Supplier<MVMap<K, V>> map, Function<MVMap<K, V>, V> function) {
        return Mono
            .fromCallable(() -> {
                int retry = 0;
                do {
                    try {
                        return function.apply(map.get());
                    } catch (MVStoreException e) {
                        initRepository();
                    }
                } while (retry++ == 0);
                return function.apply(map.get());
            })
            .subscribeOn(scheduler);
    }

    @Override
    @PreDestroy
    public void shutdown() {
        super.shutdown();
        log.info("Persistent and close session");
        Map<String, DeviceSessionRef> sessions = new HashMap<>(localSessions);
        localSessions.clear();

        Flux.fromIterable(sessions.values())
            .filter(ref -> ref.loaded != null && ref.loaded.isWrapFrom(PersistentSession.class))
            .map(ref -> ref.loaded.unwrap(PersistentSession.class))
            .as(this::tryPersistent)
            .then(
                Flux.fromIterable(sessions.values())
                    .filter(ref -> ref.loaded != null && !ref.loaded.isWrapFrom(PersistentSession.class))
                    .flatMap(ref -> closeSessionSafe(ref.loaded))
                    .then()
            )
            .block();
        log.info("Compact and close local session store {}", filePath);

        if (repository.size() < 10_0000) {
            repository.store.close(-1);
        } else {
            repository.store.close(10_000);
        }
    }

    @Override
    protected Mono<DeviceSession> handleSessionCompute(DeviceSession old, DeviceSession newSession) {
        if (old == newSession) {
            return Mono.just(newSession);
        }
        if ((old == null || !old.isWrapFrom(PersistentSession.class))
            && newSession.isWrapFrom(PersistentSession.class)) {
            return this
                .tryPersistent(Flux.just(newSession.unwrap(PersistentSession.class)))
                .thenReturn(newSession);
        }
        return super.handleSessionCompute(old, newSession);
    }

    Mono<Void> tryPersistent(Flux<PersistentSession> sessions) {

        return sessions
            .flatMap(session -> PersistentSessionData.of(registry.get(), session))
            .concatMap(e -> this
                .operateInStore(
                    () -> repository,
                    repository -> {
                        log.debug("Persistent device[{}] session", e.getDeviceId());
                        repository.put(e.getDeviceId(), e);
                        return null;
                    }))
            .onErrorResume(err -> {
                log.warn("Persistent session error", err);
                return Mono.empty();
            })
            .then();
    }

    Mono<Void> resumeSession(PersistentSessionData entity) {
        return entity
            .toSession(registry.get())
            .filterWhen(session -> {
                if (session.getOperator() == null) {
                    return Reactors.ALWAYS_FALSE;
                }
                return session
                    .getOperator()
                    .getState()
                    .map(state -> {
                        //设备已经离线?
                        if (!Objects.equals(state, DeviceState.online)) {
                            repository.remove(session.getDeviceId());
                            return false;
                        }
                        return true;
                    });
            })
            .doOnNext(session -> {
                log.debug("resume session[{}]", session.getDeviceId());
                localSessions.putIfAbsent(
                    session.getDeviceId(),
                    new DeviceSessionRef(session.getDeviceId(),
                                         this,
                                         session));
            })
            .onErrorResume((err) -> {
                log.debug("resume session[{}] error", entity.getDeviceId(), err);
                return Mono.empty();
            })
            .then();
    }

    Mono<Void> removePersistentSession(PersistentSession session) {
        return this
            .operateInStore(
                () -> repository,
                history -> repository.remove(session.getDeviceId())
            )
            .then();
    }

    @Override
    public void run(String... args) throws Exception {
        scheduler.schedule(() -> {
            int retry = 0;
            long time = System.currentTimeMillis();
            do {
                try {
                    Flux.fromIterable(repository.values())
                        .flatMap(this::resumeSession)
                        //延迟同步设备状态
                        .then(Mono.delay(stateSyncDelay))
                        .then(
                            //尝试同步历史设备状态
                            Flux.fromIterable(history.entrySet())
                                .flatMap(
                                    idAndTime -> registry
                                        .get()
                                        .getDevice(idAndTime.getKey())
                                        .flatMap(device -> device
                                            .checkState()
                                            .flatMap(state -> {
                                                //曾经在线的设备离线了.
                                                if (Objects.equals(DeviceState.offline, state)) {
                                                    log.debug("device [{}] session lost", idAndTime.getKey());
                                                    return fireEvent(
                                                        DeviceSessionEvent.of(
                                                            DeviceSessionEvent.Type.unregister,
                                                            new LostDeviceSession(idAndTime.getKey(),
                                                                                  device,
                                                                                  DefaultTransport.TCP,
                                                                                  idAndTime.getValue()),
                                                            false
                                                        )
                                                    );
                                                }
                                                return Mono.empty();
                                            })
                                        )
                                        .then(Mono.fromCallable(() -> history.remove(idAndTime.getKey()))
                                                  .subscribeOn(Schedulers.boundedElastic()))
                                    ,
                                    8)
                                .contextWrite(ctx -> ctx.put(PersistenceDeviceSessionManager.class, this))
                                .then()
                        )
                        .doFinally(ignore -> log.info(
                            "load device session cost {}ms",
                            System.currentTimeMillis() - time - stateSyncDelay.toMillis()))
                        .subscribe();
                    break;
                } catch (MVStoreException e) {
                    initRepository();
                }
            } while (retry++ == 0);
        });
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.registry = Lazy.of(() -> applicationContext.getBean(DeviceRegistry.class));
    }

    @SneakyThrows
    static ObjectInput createInput(ByteBuf buffer) {
        return Serializers.getDefault().createInput(new ByteBufInputStream(buffer, true));
    }

    @SneakyThrows
    static ObjectOutput createOutput(ByteBuf buffer) {
        return Serializers.getDefault().createOutput(new ByteBufOutputStream(buffer));
    }

    @SuppressWarnings("all")
    static class PersistentSessionDataType extends BasicDataType<PersistentSessionData> {

        @Override
        public int getMemory(PersistentSessionData obj) {
            return obj.data.length;
        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, PersistentSessionData data) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {
                data.writeExternal(output);
                output.flush();
                buff.put(buffer.nioBuffer());
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }

        @Override
        @SneakyThrows
        public void write(WriteBuffer buff, Object obj, int len) {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try (ObjectOutput output = createOutput(buffer)) {
                for (int i = 0; i < len; i++) {
                    @SuppressWarnings("all")
                    PersistentSessionData buf = ((PersistentSessionData) Array.get(obj, i));
                    buf.writeExternal(output);
                }
                output.flush();
                buff.put(buffer.nioBuffer());
            } finally {
                ReferenceCountUtil.safeRelease(buffer);
            }

        }

        @Override
        @SneakyThrows
        public void read(ByteBuffer buff, Object obj, int len) {
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                for (int i = 0; i < len; i++) {
                    PersistentSessionData data = new PersistentSessionData();
                    data.readExternal(input);
                    Array.set(obj, i, data);
                }
            }
        }

        @Override
        @SneakyThrows
        public PersistentSessionData read(ByteBuffer buff) {
            PersistentSessionData data = new PersistentSessionData();
            try (ObjectInput input = createInput(Unpooled.wrappedBuffer(buff))) {
                data.readExternal(input);
            }
            return data;
        }

        @Override
        public PersistentSessionData[] createStorage(int size) {
            return new PersistentSessionData[size];
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class PersistentSessionData implements Externalizable {
        private String provider;
        private String deviceId;
        private byte[] data;

        public Mono<PersistentSession> toSession(DeviceRegistry registry) {
            DeviceSessionProvider provider = DeviceSessionProvider
                .lookup(this.provider)
                .orElseGet(UnknownDeviceSessionProvider::getInstance);
            return provider.deserialize(data, registry);
        }

        public static Mono<PersistentSessionData> of(DeviceRegistry registry, PersistentSession session) {
            DeviceSessionProvider provider = DeviceSessionProvider
                .lookup(session.getProvider())
                .orElseGet(UnknownDeviceSessionProvider::getInstance);
            return provider
                .serialize(session, registry)
                .map(data -> {
                    PersistentSessionData persistentSessionData = new PersistentSessionData();
                    persistentSessionData.provider = session.getProvider();
                    persistentSessionData.deviceId = session.getDeviceId();
                    persistentSessionData.data = data;
                    return persistentSessionData;
                });
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(provider);
            out.writeInt(data.length);
            out.write(data);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            provider = RecyclerUtils.intern(in.readUTF());
            data = new byte[in.readInt()];
            in.readFully(data);
        }
    }

}
