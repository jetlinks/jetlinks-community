package org.jetlinks.community.protocol;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.supports.protocol.StaticProtocolSupports;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Getter
@Setter
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LazyInitManagementProtocolSupports extends StaticProtocolSupports implements CommandLineRunner {

    private ProtocolSupportManager manager;

    private ProtocolSupportLoader loader;

    private ClusterManager clusterManager;

    @Setter(AccessLevel.PRIVATE)
    private Map<String, String> configProtocolIdMapping = new ConcurrentHashMap<>();

    private Duration loadTimeOut = Duration.ofSeconds(30);

    public void init() {

        clusterManager.<ProtocolSupportDefinition>getTopic("_protocol_changed")
                      .subscribe()
                      .subscribe(protocol -> this.init(protocol).subscribe());

        try {
            manager
                .loadAll()
                .filter(de -> de.getState() == 1)
                .flatMap(this::init)
                .blockLast(loadTimeOut);
        } catch (Throwable e) {
            log.error("load protocol error", e);
        }

    }

    public Mono<Void> init(ProtocolSupportDefinition definition) {
        try {
            if (definition.getState() != 1) {
                String protocol = configProtocolIdMapping.get(definition.getId());
                if (protocol != null) {
                    log.debug("uninstall protocol:{}", definition);
                    unRegister(protocol);
                    return Mono.empty();
                }
            }
            String operation = definition.getState() != 1 ? "uninstall" : "install";
            Consumer<ProtocolSupport> consumer = definition.getState() != 1 ? this::unRegister : this::register;

            log.debug("{} protocol:{}", operation, definition);

            return loader
                .load(definition)
                .doOnNext(e -> {
                    e.init(definition.getConfiguration());
                    log.debug("{} protocol[{}] success: {}", operation, definition.getId(), e);
                    configProtocolIdMapping.put(definition.getId(), e.getId());
                    consumer.accept(e);
                })
                .onErrorResume((e) -> {
                    log.error("{} protocol[{}] error: {}", operation, definition.getId(), e.getLocalizedMessage());
                    return Mono.empty();
                })
                .then();
        } catch (Throwable err) {
            log.error("init protocol error", err);
        }
        return Mono.empty();
    }

    @Override
    public void run(String... args) {
        init();
    }
}
