package org.jetlinks.community.configure.device;

import io.scalecube.services.ServiceCall;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.PersistentSession;
import org.jetlinks.supports.device.session.MicroserviceDeviceSessionManager;
import org.jetlinks.supports.scalecube.ExtendedCluster;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.util.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class PersistenceDeviceSessionManager extends MicroserviceDeviceSessionManager implements CommandLineRunner, ApplicationContextAware {
    private Supplier<DeviceRegistry> registry;

    private final ReactiveRepository<PersistentSessionEntity, String> repository;

    public PersistenceDeviceSessionManager(ExtendedCluster cluster,
                                           ServiceCall serviceCall,
                                           ReactiveRepository<PersistentSessionEntity, String> repository) {
        super(cluster, serviceCall);
        this.repository = repository;
    }

    @Override
    public void init() {

        super.init();
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
            .flatMap(Function.identity())
            .filter(session -> session.isWrapFrom(PersistentSession.class))
            .map(session -> session.unwrap(PersistentSession.class))
            .as(this::tryPersistent)
            .block();
    }

    @Override
    protected Mono<DeviceSession> handleSessionCompute(DeviceSession old, DeviceSession newSession) {
        if (old == newSession) {
            return Mono.just(newSession);
        }
        if ((old == null || !old.isWrapFrom(PersistentSession.class))
            && newSession.isWrapFrom(PersistentSession.class)) {
            //todo 批量处理?
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
            .as(repository::save)
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
                localSessions.putIfAbsent(session.getDeviceId(), Mono.just(session));
            })
            .onErrorResume((err) -> {
                log.debug("resume session[{}] error", entity.getDeviceId(), err);
                return Mono.empty();
            })
            .then();
    }

    Mono<Void> removePersistentSession(PersistentSession session) {
        return repository
            .deleteById(session.getId())
            .then();
    }

    @Override
    public void run(String... args) throws Exception {
        repository
            .createQuery()
            .where(PersistentSessionEntity::getServerId, getCurrentServerId())
            .fetch()
            .flatMap(this::resumeSession)
            .subscribe();
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.registry = Lazy.of(() -> applicationContext.getBean(DeviceRegistry.class));
    }
}
