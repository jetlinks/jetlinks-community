package org.jetlinks.community.notify;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@SuppressWarnings("all")
public class DefaultNotifierManager implements NotifierManager, BeanPostProcessor, CommandLineRunner {

    private final Map<String, Map<String, NotifierProvider>> providers = new ConcurrentHashMap<>();

    private Map<String, Notifier> notifiers = new ConcurrentHashMap<>();

    private NotifyConfigManager configManager;

    private EventBus eventBus;

    public DefaultNotifierManager(EventBus eventBus, NotifyConfigManager manager) {
        this.configManager = manager;
        this.eventBus = eventBus;
    }

    protected Mono<NotifierProperties> getProperties(NotifyType notifyType,
                                                     String id) {
        return configManager.getNotifyConfig(notifyType, id);
    }

    public Mono<Void> reload(String id) {
        return this
            .doReload(id)
            .then(eventBus.publish("/_sys/notifier/reload", id))
            .then();
    }

    private Mono<String> doReload(String id) {
        log.debug("reload notifer config {}",id);
        return Mono
            .justOrEmpty(notifiers.remove(id))
            .flatMap(Notifier::close)
            .thenReturn(id);
    }

    @Nonnull
    public Mono<Notifier> createNotifier(NotifierProperties properties) {
        return Mono
            .justOrEmpty(providers.get(properties.getType()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的通知类型:" + properties.getType())))
            .flatMap(map -> Mono.justOrEmpty(map.get(properties.getProvider())))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的服务商:" + properties.getProvider())))
            .flatMap(notifierProvider -> notifierProvider.createNotifier(properties))
            //转成代理,把通知事件发送到消息网关中.
            .map(notifier -> new NotifierEventDispatcher<>(eventBus, notifier))
            .flatMap(notifier -> Mono.justOrEmpty(notifiers.put(properties.getId(), notifier))
                                     .flatMap(Notifier::close)//如果存在旧的通知器则关掉之
                                     .thenReturn(notifier));
    }

    @Override
    @Nonnull
    public Mono<Notifier> getNotifier(@Nonnull NotifyType type,
                                      @Nonnull String id) {
        return Mono
            .justOrEmpty(notifiers.get(id))
            .switchIfEmpty(Mono.defer(() -> this.getProperties(type, id).flatMap(this::createNotifier)));
    }

    public void registerProvider(NotifierProvider provider) {
        providers.computeIfAbsent(provider.getType().getId(), ignore -> new ConcurrentHashMap<>())
                 .put(provider.getProvider().getId(), provider);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof NotifierProvider) {
            registerProvider(((NotifierProvider) bean));
        }
        return bean;
    }

    @Override
    public void run(String... args) throws Exception {

        eventBus
            .subscribe(
                Subscription.builder()
                            .subscriberId("notifier-loader")
                            .topics("/_sys/notifier/reload")
                            .justBroker()
                            .build(),
                String.class
            )
            .flatMap(id -> this
                .doReload(id)
                .onErrorResume(err -> {
                    log.error("reload notifer config error", err);
                    return Mono.empty();
                }))
            .subscribe();

    }
}
