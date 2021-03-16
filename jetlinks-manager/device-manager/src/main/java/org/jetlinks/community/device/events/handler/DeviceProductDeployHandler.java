package org.jetlinks.community.device.events.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.community.device.events.DeviceProductDeployEvent;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
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


    @Autowired
    public DeviceProductDeployHandler(LocalDeviceProductService productService,
                                      DeviceDataService dataService) {
        this.productService = productService;
        this.dataService = dataService;
    }


    @EventListener
    public void handlerEvent(DeviceProductDeployEvent event) {
        event.async(
            this
                .doRegisterMetadata(event.getId(), event.getMetadata())
        );
    }

    private Mono<Void> doRegisterMetadata(String productId, String metadataString) {
        return codec
            .decode(metadataString)
            .flatMap(metadata -> dataService.registerMetadata(productId, metadata));
    }


    @Override
    public void run(String... args) {
        //启动时发布物模型
        productService
            .createQuery()
            .fetch()
            .filter(product -> new Byte((byte) 1).equals(product.getState()))
            .flatMap(deviceProductEntity -> this
                .doRegisterMetadata(deviceProductEntity.getId(), deviceProductEntity.getMetadata())
                .onErrorResume(err -> {
                    log.warn("register product metadata error", err);
                    return Mono.empty();
                })
            )
            .subscribe();
    }
}
