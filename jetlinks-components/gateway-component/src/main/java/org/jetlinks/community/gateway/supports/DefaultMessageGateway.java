package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.gateway.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Slf4j
public class DefaultMessageGateway implements MessageGateway {

    @Getter
    private final String id;
    @Getter
    private final String name;

    private final TopicPart root = new TopicPart(null, "/");

    private final Map<String, ConnectionSession> sessions = new ConcurrentHashMap<>();

    private final ClientSessionManager sessionManager;

    private final Map<String, Connector> connectors = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();

    private final LocalMessageConnector localGatewayConnector;

    public DefaultMessageGateway(String id, ClientSessionManager sessionManager) {
        this(id, id, sessionManager);
    }

    public DefaultMessageGateway(String id, String name, ClientSessionManager sessionManager) {
        this.id = id;
        this.name = name;
        this.sessionManager = sessionManager;
        this.localGatewayConnector = new LocalMessageConnector();
        this.registerMessageConnector(localGatewayConnector);
    }

    private final AtomicLong nextSubCounter = new AtomicLong();

    @Override
    public String nextSubscriberId(String prefix) {
        return prefix + ":" + nextSubCounter.getAndIncrement();
    }
    @Override
    public Flux<ClientSession> publish(TopicMessage message, boolean shareCluster) {
        return publishLocal(message, session -> true);
    }

    @Override
    public Flux<TopicMessage> subscribe(Collection<Subscription> subscriptions, boolean shareCluster) {
        return subscribe(subscriptions, "local:".concat(IDGenerator.SNOW_FLAKE_STRING.generate()), shareCluster);
    }

    @Override
    public Flux<TopicMessage> subscribe(Collection<Subscription> subscriptions, String id, boolean shareCluster) {
        return Flux.defer(() -> {
            LocalMessageConnection networkConnection = localGatewayConnector.addConnection(id, shareCluster);
            return networkConnection
                .onLocalMessage()
                .doOnSubscribe(sub -> subscriptions.forEach(networkConnection::addSubscription))
                .doFinally((s) -> networkConnection.disconnect());
        });
    }

    @Override
    public void registerMessageConnector(MessageConnector connector) {
        if (null != removeConnector(connector.getId())) {
            log.warn("connector exists , shutdown it !");
        }

        Connector _connector = new Connector(connector);
        connectors.put(connector.getId(), _connector);

        if (started.get()) {
            _connector.startup();
        }
    }

    @Override
    public MessageConnector removeConnector(String connectorId) {

        Connector connector = connectors.remove(connectorId);
        if (connector != null) {
            connector.shutdown();
            return connector.connector;
        }
        return null;
    }

    private Flux<ClientSession> publishLocal(TopicMessage message,
                                             Predicate<ConnectionSession> filter) {
        return Flux.defer(() -> root.find(message.getTopic())
            .flatMapIterable(TopicPart::getSessionId)
            .flatMap(id -> Mono.justOrEmpty(sessions.get(id)))
            .distinct(ConnectionSession::getId)
            .filter(connectionSession -> connectionSession.isAlive() && filter.test(connectionSession))
            .flatMap(session ->
                session.connection
                    .asSubscriber()
                    .flatMap(subscriber -> subscriber.publish(message))
                    .doOnSuccess(nil -> {
                        log.debug("publish message [{}] to session:[{}] complete", message.getTopic(), session.getId());
                    })
                    .onErrorContinue((err, se) -> {
                        log.error("publish message [{}] to session:[{}] error", message.getTopic(), session.getId(), err);
                    })
                    .thenReturn(session))
            .map(ConnectionSession::getSession))
            ;
    }

    @Override
    public void startup() {
        if (!started.getAndSet(true)) {
            for (Connector value : connectors.values()) {
                if (value.disposable == null) {
                    value.startup();
                }
            }
        }
    }

    @Override
    public void shutdown() {
        started.set(false);
        for (Connector value : connectors.values()) {
            value.shutdown();
        }
    }

    private Mono<Void> dispatch(ConnectionSession from, TopicMessage message) {
        //转发到其他topic
        return publishLocal(message, session -> true).then();
    }

    @Getter
    class ConnectionSession {
        String id;
        Connector connector;
        ClientSession session;

        MessageConnection connection;

        Disposable disposable;

        boolean onlyConsumeLocal;

        boolean isAlive() {
            return connection.isAlive();
        }

        void init() {
            disposable = connection
                .asSubscriber()
                .subscribe(subscriber -> {
                    subscriber
                        .onSubscribe()
                        .takeWhile(r -> isAlive())
                        .flatMap(subscription -> {
                            if (log.isDebugEnabled()) {
                                log.debug("session:[{}] subscribe:[{}]", session.getId(), subscription.getTopic());
                            }
                            root.subscribe(subscription.getTopic())
                                .addSessionId(getId());
                            return session.addSubscription(subscription)
                                .thenReturn(subscription);
                        }).subscribe();
                    subscriber.onUnSubscribe()
                        .takeWhile(r -> isAlive())
                        .flatMap(subscription ->
                            root.get(subscription.getTopic())
                                .doOnNext(part -> {
                                    if (log.isDebugEnabled()) {
                                        log.debug("session:[{}] unsubscribe:[{}]", session.getId(), part.getTopic());
                                    }
                                    part.removeSession(getId());
                                })
                                .then(session.removeSubscription(subscription)))
                        .subscribe();
                });
            //加载会话已有的订阅信息
            session.getSubscriptions()
                .map(Subscription::getTopic)
                .flatMap(topic -> root.get(topic))
                .subscribe(part -> part.addSessionId(getId()));
        }

        void close() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            sessions.remove(getId());
            //取消订阅
            session.getSubscriptions()
                .map(Subscription::getTopic)
                .flatMap(topic -> root.get(topic))
                .doOnNext(part -> part.removeSession(getId()))
                .then(sessionManager.closeSession(DefaultMessageGateway.this.getId(), getId()))
                .doFinally(s -> log.debug("session [{}] closed", getId()))
                .subscribe();
        }

    }

    class Connector {
        private MessageConnector connector;

        private Disposable disposable;

        public Connector(MessageConnector connector) {
            this.connector = connector;
        }

        private void shutdown() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                disposable = null;
            }
        }

        public void startup() {
            shutdown();
            disposable = connector
                .onConnection()
                .flatMap(connection -> sessionManager
                    .createSession(getId(), connection)
                    .map(session -> {
                        ConnectionSession connectionSession = new ConnectionSession();
                        connectionSession.connection = connection;
                        connectionSession.onlyConsumeLocal = connector instanceof LocalMessageConnector;
                        connectionSession.session = session;
                        connectionSession.connector = this;
                        connectionSession.id = session.getId();
                        return connectionSession;
                    }))
                .filter(ConnectionSession::isAlive)
                .doOnNext(session -> {
                    sessions.put(session.getId(), session);
                    session.init();
                    session.connection.onDisconnect(session::close);
                    session.getConnection()
                        .asPublisher()
                        .flatMapMany(MessagePublisher::onMessage)
                        .takeWhile(r -> disposable != null)
                        .flatMap(msg -> dispatch(session, msg))
                        .onErrorContinue((err, obj) -> {
                            log.error(err.getMessage(), err);
                        })
                        .subscribe();
                })
                .subscribe();
        }
    }
}
