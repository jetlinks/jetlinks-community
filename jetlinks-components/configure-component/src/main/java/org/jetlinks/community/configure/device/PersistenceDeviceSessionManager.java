package org.jetlinks.community.configure.device;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.jetlinks.community.configure.cluster.Cluster;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.supports.device.session.ClusterDeviceSessionManager;
import org.jetlinks.supports.utils.MVStoreUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
public class PersistenceDeviceSessionManager extends ClusterDeviceSessionManager implements CommandLineRunner, ApplicationContextAware {
    private Supplier<DeviceRegistry> registry;

    private MVMap<String, PersistentSessionEntity> repository;

    @Getter
    @Setter
    private String filePath;

    @Getter
    @Setter
    private Duration flushInterval = Duration.ofMinutes(10);

    public PersistenceDeviceSessionManager(RpcManager rpcManager) {
        super(rpcManager);
    }

    static MVMap<String, PersistentSessionEntity> initStore(String file) {
        MVStore store =
            MVStoreUtils.open(
                new File(file),
                "device-session",
                builder -> {
                    return builder.cacheSize(1);
                });

        return MVStoreUtils.openMap(store, "device-session", new MVMap.Builder<>());
    }

    @Override
    public void init() {
        super.init();
        if (filePath == null) {
            filePath = "./data/sessions-" + (Cluster
                .id()
                .replace(":", "_")
                .replace("/", ""));
        }
        repository = initStore(filePath);

        if (!flushInterval.isZero() && !flushInterval.isNegative()) {
            disposable.add(
                Flux
                    .interval(flushInterval)
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
            listenEvent(event -> {
                //移除持久化的会话
                if (event.getType() == DeviceSessionEvent.Type.unregister
                    && event.getSession().isWrapFrom(PersistentSession.class)) {
                    return removePersistentSession(
                        event.getSession().unwrap(PersistentSession.class)
                    );
                }
                return Mono.empty();
            })
        );
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Flux.fromIterable(localSessions.values())
            .filter(ref -> ref.loaded != null)
            .filter(ref -> ref.loaded.isWrapFrom(PersistentSession.class))
            .map(ref -> ref.loaded.unwrap(PersistentSession.class))
            .as(this::tryPersistent)
            .block();
        repository.store.close(-1);
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
            .flatMap(session -> PersistentSessionEntity.from(getCurrentServerId(), session, registry.get()))
            .distinct(PersistentSessionEntity::getId)
            .doOnNext(e -> {
                log.debug("persistent device[{}] session", e.getDeviceId());
                repository.put(e.getDeviceId(), e);
            })
            .onErrorResume(err -> {
                log.warn("persistent session error", err);
                return Mono.empty();
            })
            .then();
    }

    Mono<Void> resumeSession(PersistentSessionEntity entity) {
        return entity
            .toSession(registry.get())
            .doOnNext(session -> {
                log.debug("resume session[{}]", session.getDeviceId());
                localSessions.putIfAbsent(session.getDeviceId(),
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
        repository.remove(session.getId());
        return Mono.empty();
    }

    @Override
    public void run(String... args) throws Exception {

        Flux.fromIterable(repository.values())
            .flatMap(this::resumeSession)
            .subscribe();
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.registry = Lazy.of(() -> applicationContext.getBean(DeviceRegistry.class));
    }
}
