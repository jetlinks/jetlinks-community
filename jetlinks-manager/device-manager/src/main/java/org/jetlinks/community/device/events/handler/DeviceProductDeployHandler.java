package org.jetlinks.community.device.events.handler;

import lombok.extern.slf4j.Slf4j;
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

/**
 * 处理设备型号发布事件
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

    private DeviceMetadataCodec codec = new JetLinksDeviceMetadataCodec();

    private final TimeSeriesManager timeSeriesManager;

    @Autowired
    public DeviceProductDeployHandler(LocalDeviceProductService productService, TimeSeriesManager timeSeriesManager) {
        this.productService = productService;
        this.timeSeriesManager = timeSeriesManager;
    }

    @EventListener
    public void handlerEvent(DeviceProductDeployEvent event) {
        initDeviceEventTimeSeriesMetadata(event.getMetadata(), event.getId());
        initDevicePropertiesTimeSeriesMetadata(event.getId());
        initDeviceLogTimeSeriesMetadata(event.getId());
    }

    private void initDeviceEventTimeSeriesMetadata(String metadata, String productId) {
        codec.decode(metadata)
            .flatMapIterable(DeviceMetadata::getEvents)
            .flatMap(eventMetadata -> timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.event(productId, eventMetadata)))
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }

    private void initDevicePropertiesTimeSeriesMetadata(String productId) {
        timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.properties(productId))
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }

    private void initDeviceLogTimeSeriesMetadata(String productId) {
        timeSeriesManager.registerMetadata(DeviceTimeSeriesMetadata.log(productId))
            .doOnError(err -> log.error(err.getMessage(), err))
            .subscribe();
    }

    @Override
    public void run(String... args) throws Exception {
        productService.createQuery()
            .fetch()
            .filter(productService -> new Byte((byte) 1).equals(productService.getState()))
            .subscribe(deviceProductEntity -> {
                initDeviceEventTimeSeriesMetadata(deviceProductEntity.getMetadata(), deviceProductEntity.getId());
                initDevicePropertiesTimeSeriesMetadata(deviceProductEntity.getId());
                initDeviceLogTimeSeriesMetadata(deviceProductEntity.getId());
            });
    }
}
