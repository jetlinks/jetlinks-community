package org.jetlinks.community.device.events.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.service.data.DeviceLatestDataService;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;

/**
 * 处理设备产品发布事件
 *
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Component
@Slf4j
@Order(1)
public class DeviceProductDeployHandler implements CommandLineRunner {

    private final LocalDeviceProductService productService;

    private final DeviceMetadataCodec codec = new JetLinksDeviceMetadataCodec();

    private final DeviceDataService dataService;

    private final DeviceLatestDataService latestDataService;
    private final EventBus eventBus;

    private final Disposable disposable;

    @Autowired
    public DeviceProductDeployHandler(LocalDeviceProductService productService,
                                      DeviceDataService dataService,
                                      EventBus eventBus,
                                      DeviceLatestDataService latestDataService) {
        this.productService = productService;
        this.dataService = dataService;
        this.eventBus = eventBus;
        this.latestDataService = latestDataService;
        //监听其他服务器上的物模型变更
        disposable = eventBus
            .subscribe(Subscription
                           .builder()
                           .subscriberId("product-metadata-upgrade")
                           .topics("/_sys/product-upgrade")
                           .justBroker()
                           .build(), String.class)
            .flatMap(id -> this
                .reloadMetadata(id)
                .onErrorResume((err) -> {
                    log.warn("handle product upgrade event error", err);
                    return Mono.empty();
                }))
            .subscribe();
    }

    @PreDestroy
    public void shutdown() {
        disposable.dispose();
    }

    @EventListener
    public void handlerEvent(DeviceProductDeployEvent event) {
        event.async(
            this
                .doRegisterMetadata(event.getId(), event.getMetadata())
                .then(
                    eventBus.publish("/_sys/product-upgrade", event.getId())
                )
        );
    }

    protected Mono<Void> reloadMetadata(String productId) {
        return productService
            .findById(productId)
            .flatMap(product -> doReloadMetadata(productId, product.getMetadata()))
            .then();
    }

    protected Mono<Void> doReloadMetadata(String productId, String metadataString) {
        return codec
            .decode(metadataString)
            .flatMap(metadata -> Flux
                .mergeDelayError(2,
                                 dataService.reloadMetadata(productId, metadata),
                                 latestDataService.reloadMetadata(productId, metadata))
                .then());
    }

    protected Mono<Void> doRegisterMetadata(String productId, String metadataString) {
        return codec
            .decode(metadataString)
            .flatMap(metadata -> Flux
                .mergeDelayError(2,
                                 dataService.registerMetadata(productId, metadata),
                                 latestDataService.upgradeMetadata(productId, metadata))
                .then());
    }


    @Override
    public void run(String... args) {
        productService
            .createQuery()
            .fetch()
            .filter(product -> new Byte((byte) 1).equals(product.getState()))
            .flatMap(deviceProductEntity -> this
                .doRegisterMetadata(deviceProductEntity.getId(), deviceProductEntity.getMetadata())
                .onErrorResume(err -> {
                    log.warn("register product [{}] metadata error", deviceProductEntity.getId(), err);
                    return Mono.empty();
                })
            )
            .subscribe();
    }
}
