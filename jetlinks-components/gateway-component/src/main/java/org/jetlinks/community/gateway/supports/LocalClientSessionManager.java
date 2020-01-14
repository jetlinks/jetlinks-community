package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.ClientSession;
import org.jetlinks.community.gateway.ClientSessionManager;
import org.jetlinks.community.gateway.MessageConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalClientSessionManager implements ClientSessionManager {

    private Map<String, Map<String, LocalClientSession>> sessionStore = new ConcurrentHashMap<>();

    protected Map<String, LocalClientSession> getGateWaySessionStore(String gateway) {
        return sessionStore.computeIfAbsent(gateway, __ -> new ConcurrentHashMap<>());
    }

    @Override
    public Mono<ClientSession> createSession(String messageGatewayId, MessageConnection connection) {
        return Mono.fromSupplier(() -> {
            LocalClientSession session = new LocalClientSession(connection.getId());

            getGateWaySessionStore(messageGatewayId).put(connection.getId(), session);
            return session;
        });
    }

    @Override
    public Flux<ClientSession> getSessions(String messageGatewayId) {
        return Flux.fromIterable(getGateWaySessionStore(messageGatewayId).values());
    }

    @Override
    public Mono<ClientSession> getSession(String messageGatewayId, String id) {
        return Mono.justOrEmpty(getGateWaySessionStore(messageGatewayId).get(id));
    }

    @Override
    public Flux<ClientSession> getSessions(String gateway, Collection<String> id) {
        Map<String, LocalClientSession> store = getGateWaySessionStore(gateway);

        return Flux.fromIterable(id)
                .flatMap(_id -> Mono.justOrEmpty(store.get(_id)));
    }

    @Override
    public Mono<Void> closeSession(String gateway, String id) {

        return Mono.fromRunnable(() -> {
            Optional.ofNullable(getGateWaySessionStore(gateway).remove(id))
                    .ifPresent(LocalClientSession::close);
        });
    }
}
