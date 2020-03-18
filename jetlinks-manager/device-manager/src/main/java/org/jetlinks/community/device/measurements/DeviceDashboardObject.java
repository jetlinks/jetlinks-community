package org.jetlinks.community.device.measurements;

import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.community.dashboard.DashboardObject;
import org.jetlinks.community.dashboard.Measurement;
import org.jetlinks.community.dashboard.ObjectDefinition;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DeviceDashboardObject implements DashboardObject {
    private String id;

    private String name;

    private DeviceProductOperator productOperator;

    private MessageGateway messageGateway;

    private TimeSeriesManager timeSeriesManager;

    private DeviceDashboardObject(String id, String name,
                                  DeviceProductOperator productOperator,
                                  MessageGateway messageGateway, TimeSeriesManager timeSeriesManager) {
        this.id = id;
        this.name = name;
        this.productOperator = productOperator;
        this.messageGateway = messageGateway;
        this.timeSeriesManager = timeSeriesManager;
    }

    public static DeviceDashboardObject of(String id, String name,
                                           DeviceProductOperator productOperator,
                                           MessageGateway messageGateway, TimeSeriesManager timeSeriesManager) {
        return new DeviceDashboardObject(id, name, productOperator, messageGateway, timeSeriesManager);
    }

    @Override
    public ObjectDefinition getDefinition() {
        return new ObjectDefinition() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @Override
    public Flux<Measurement> getMeasurements() {
        return Flux.concat(

            productOperator.getMetadata()
                .flatMapIterable(DeviceMetadata::getEvents)
                .map(event -> new DeviceEventMeasurement(messageGateway, event, timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceEventMetric(id, event.getId())))),

            productOperator.getMetadata()
                .map(metadata -> new DevicePropertiesMeasurement(messageGateway, metadata, timeSeriesManager.getService(DeviceTimeSeriesMetric.devicePropertyMetric(id)))),

            productOperator.getMetadata()
                .map(metadata -> new DeviceEventsMeasurement(productOperator.getId(), messageGateway, metadata, timeSeriesManager)),

            productOperator.getMetadata()
                .flatMapIterable(DeviceMetadata::getProperties)
                .map(event -> new DevicePropertyMeasurement(messageGateway, event, timeSeriesManager.getService(DeviceTimeSeriesMetric.devicePropertyMetric(id))))
        );
    }

    @Override
    public Mono<Measurement> getMeasurement(String id) {
        if ("properties".equals(id)) {
            return productOperator.getMetadata()
                .map(metadata -> new DevicePropertiesMeasurement(messageGateway, metadata, timeSeriesManager.getService(DeviceTimeSeriesMetric.devicePropertyMetric(this.id))));
        }
        if ("events".equals(id)) {
            return productOperator.getMetadata()
                .map(metadata -> new DeviceEventsMeasurement(productOperator.getId(), messageGateway, metadata, timeSeriesManager));
        }
        return productOperator.getMetadata()
            .flatMap(metadata -> Mono.justOrEmpty(metadata.getEvent(id)))
            .<Measurement>map(event -> new DeviceEventMeasurement(messageGateway, event, timeSeriesManager.getService(DeviceTimeSeriesMetric.deviceEventMetric(this.id, event.getId()))))
            //事件没获取到则尝试获取属性
            .switchIfEmpty(productOperator.getMetadata()
                .flatMap(metadata -> Mono.justOrEmpty(metadata.getProperty(id)))
                .map(event -> new DevicePropertyMeasurement(messageGateway, event, timeSeriesManager.getService(DeviceTimeSeriesMetric.devicePropertyMetric(this.id)))));
    }
}
