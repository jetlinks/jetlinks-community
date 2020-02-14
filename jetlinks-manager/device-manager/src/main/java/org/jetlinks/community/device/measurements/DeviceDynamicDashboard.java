package org.jetlinks.community.device.measurements;

import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
public class DeviceDynamicDashboard implements DeviceDashboard {

    @Autowired
    private LocalDeviceProductService productService;

    @Autowired
    private DeviceRegistry registry;

    @Autowired
    private MessageGateway messageGateway;

    @Autowired
    private TimeSeriesManager timeSeriesManager;

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
            .map(operator -> DeviceDashboardObject.of(product.getId(), product.getName(), operator, messageGateway, timeSeriesManager));
    }
}
