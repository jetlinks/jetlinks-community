package org.jetlinks.community.device.measurements;

import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
public class DeviceDynamicDashboard implements DeviceDashboard {

    private final LocalDeviceProductService productService;

    private final DeviceRegistry registry;

    private final EventBus eventBus;

    private final TimeSeriesManager timeSeriesManager;

    public DeviceDynamicDashboard(LocalDeviceProductService productService,
                                  DeviceRegistry registry,
                                  EventBus eventBus,
                                  TimeSeriesManager timeSeriesManager) {
        this.productService = productService;
        this.registry = registry;
        this.eventBus = eventBus;
        this.timeSeriesManager = timeSeriesManager;
    }

    @PostConstruct
    public void init() {
        //设备状态变更
    }

    @Override
    public Flux<DashboardObject> getObjects() {
        return productService.createQuery()
            .fetch()
            .flatMap(this::convertObject);
    }

    @Override
    public Mono<DashboardObject> getObject(String id) {
        return productService.findById(id)
            .flatMap(this::convertObject);
    }

    protected Mono<DeviceDashboardObject> convertObject(DeviceProductEntity product) {
        return registry.getProduct(product.getId())
            .map(operator -> DeviceDashboardObject.of(product.getId(), product.getName(), operator, eventBus, timeSeriesManager));
    }
}
