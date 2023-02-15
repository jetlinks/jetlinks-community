package org.jetlinks.community.device.message.transparent;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.ValidationException;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DirectDeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.community.OperationSource;
import org.jetlinks.community.device.entity.TransparentMessageCodecEntity;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class TransparentDeviceMessageConnector implements CommandLineRunner, DeviceMessageSenderInterceptor {

    private final ReactiveRepository<TransparentMessageCodecEntity, String> repository;

    private final DecodedClientMessageHandler messageHandler;

    private final EventBus eventBus;

    private final Map<CacheKey, TransparentMessageCodec> codecs = new NonBlockingHashMap<>();

    public TransparentDeviceMessageConnector(@SuppressWarnings("all")
                                                 ReactiveRepository<TransparentMessageCodecEntity, String> repository,
                                             DecodedClientMessageHandler messageHandler,
                                             EventBus eventBus,
                                             ObjectProvider<TransparentMessageCodecProvider> providers) {
        this.repository = repository;
        this.messageHandler = messageHandler;
        this.eventBus = eventBus;
        for (TransparentMessageCodecProvider provider : providers) {
            TransparentMessageCodecProviders.addProvider(provider);
        }
    }


    @Subscribe("/device/*/*/message/direct")
    public Mono<Void> handleMessage(DirectDeviceMessage message) {
        String productId = message.getHeaderOrDefault(Headers.productId);
        String deviceId = message.getDeviceId();
        TransparentMessageCodec codec = getCodecOrNull(productId, deviceId);
        if (null == codec) {
            return Mono.empty();
        }
        return codec
            .decode(message)
            .flatMap(msg -> messageHandler.handleMessage(null, msg))
            .then();
    }

    private TransparentMessageCodec getCodecOrNull(String productId, String deviceId) {
        CacheKey cacheKey = new CacheKey(productId, deviceId);
        TransparentMessageCodec codec = codecs.get(cacheKey);
        if (codec == null) {
            cacheKey.setDeviceId(null);
            codec = codecs.get(cacheKey);
        }
        return codec;
    }

    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        return device
            .getSelfConfig(DeviceConfigKey.productId)
            .mapNotNull(productId -> getCodecOrNull(productId, device.getDeviceId()))
            .<DeviceMessage>flatMap(codec -> codec
                .encode(message)
                .doOnNext(msg -> {
                    msg.addHeader("encodeBy", message.getMessageType().name());
                    //所有透传消息都设置为异步
                    msg.addHeader(Headers.async, true);
                   // msg.addHeader(Headers.sendAndForget, true);
                })
            )
            .defaultIfEmpty(message);
    }


    @Subscribe(value = "/_sys/transparent-codec/load", features = Subscription.Feature.broker)
    public Mono<Void> doLoadCodec(TransparentMessageCodecEntity entity) {
        CacheKey key = new CacheKey(entity.getProductId(), entity.getDeviceId());
        TransparentMessageCodecProvider provider = TransparentMessageCodecProviders
            .getProvider(entity.getProvider())
            .orElseThrow(() -> new ValidationException("codec", "error.unsupported_codec", entity.getProvider()));
        return provider
            .createCodec(entity.getConfiguration())
            .doOnNext(codec -> codecs.put(key, codec))
            .contextWrite(OperationSource.ofContext(entity.getId(),null,entity))
            .switchIfEmpty(Mono.fromRunnable(() -> codecs.remove(key)))
            .then();
    }

    @Subscribe(value = "/_sys/transparent-codec/removed", features = Subscription.Feature.broker)
    public Mono<Void> doRemoveCodec(TransparentMessageCodecEntity entity) {
        CacheKey key = new CacheKey(entity.getProductId(), entity.getDeviceId());
        codecs.remove(key);
        return Mono.empty();
    }

    @EventListener
    public void handleEntityEvent(EntityCreatedEvent<TransparentMessageCodecEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::loadCodec)
        );
    }

    @EventListener
    public void handleEntityEvent(EntitySavedEvent<TransparentMessageCodecEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::loadCodec)
        );
    }

    @EventListener
    public void handleEntityEvent(EntityModifyEvent<TransparentMessageCodecEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(this::loadCodec)
        );
    }

    @EventListener
    public void handleEntityEvent(EntityDeletedEvent<TransparentMessageCodecEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::removeCodec)
        );
    }

    public Mono<Void> loadCodec(TransparentMessageCodecEntity entity) {
        return doLoadCodec(entity)
            .then(
                eventBus
                    .publish("/_sys/transparent-codec/load", entity)
                    .then()
            );
    }

    public Mono<Void> removeCodec(TransparentMessageCodecEntity entity) {
        return doRemoveCodec(entity)
            .then(
                eventBus
                    .publish("/_sys/transparent-codec/removed", entity)
                    .then()
            );
    }

    @Override
    public void run(String... args) throws Exception {
        repository
            .createQuery()
            .fetch()
            .flatMap(e -> this
                .doLoadCodec(e)
                .onErrorResume(err -> {
                    log.error("load transparent device message codec [{}:{}] error", e.getId(), e.getProvider(), err);
                    return Mono.empty();
                }))
            .subscribe();
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    static class CacheKey {
        private String productId;
        private String deviceId;
    }
}
