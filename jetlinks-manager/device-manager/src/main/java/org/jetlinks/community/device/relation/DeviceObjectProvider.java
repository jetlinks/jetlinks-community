package org.jetlinks.community.device.relation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.impl.SimpleObjectType;
import org.jetlinks.community.relation.impl.property.PropertyOperationStrategy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Getter
@Setter
@AllArgsConstructor
@Component
public class DeviceObjectProvider implements RelationObjectProvider {

    private final DeviceDataManager deviceDataManager;

    private final LocalDeviceInstanceService instanceService;

    private final DeviceRegistry registry;

    @Override
    public String getTypeId() {
        return RelationObjectProvider.TYPE_DEVICE;
    }

    @Override
    public Mono<ObjectType> getType() {
        return Mono.just(new SimpleObjectType(getTypeId(), "设备", "设备"));
    }

    @Override
    public PropertyOperation properties(String id) {
        return PropertyOperationStrategy
            .composite(
                PropertyOperationStrategy
                    .simple(registry.getDevice(id),
                            strategy -> strategy
                                .addMapper("id", DeviceOperator::getDeviceId)
                                .addAsyncMapper(PropertyConstants.deviceName, DeviceOperator::getSelfConfig)
                                .addAsyncMapper(PropertyConstants.productName, DeviceOperator::getSelfConfig)
                                .addAsyncMapper(PropertyConstants.productId, DeviceOperator::getSelfConfig)
                                .addAsyncMapper(DeviceConfigKey.deviceType, DeviceOperator::getSelfConfig)
                                .addAsyncMapper(DeviceConfigKey.parentGatewayId, DeviceOperator::getSelfConfig)
                                .addAsyncMapper(DeviceConfigKey.firstPropertyTime, DeviceOperator::getSelfConfig)),
                PropertyOperationStrategy
                    .detect(strategy -> {
                        // dev@device.property.temp
                        // dev@device.property.temp.timestamp
                        strategy
                            .addOperation("property",
                                          key -> getDeviceProperty(id, key))
                            // dev@device.tag.tagKey
                            .addOperation("tag",
                                          key -> deviceDataManager
                                              .getTags(id, key)
                                              .map(DeviceDataManager.TagValue::getValue)
                                              .singleOrEmpty()
                            )
                            // dev@device.config.key
                            .addOperation("config",
                                          key -> registry
                                              .getDevice(id)
                                              .flatMap(device -> device.getConfig(key))
                            );
                    })
            );
    }

    protected Mono<Object> getDeviceProperty(String deviceId, String property) {
        if (!property.contains(".")) {
            return this
                .getPropertyValue(deviceId, property)
                .map(DeviceDataManager.PropertyValue::getValue);
        }
        String[] arr = property.split("[.]");
        String propertyKey = arr[0];
        String valueType = arr[1];
        Mono<DeviceDataManager.PropertyValue> propertyValueMono = this.getPropertyValue(deviceId, propertyKey);

        if ("timestamp".equals(valueType)) {
            return propertyValueMono
                .map(DeviceDataManager.PropertyValue::getTimestamp);
        }
        if ("state".equals(valueType)) {
            return propertyValueMono
                .mapNotNull(DeviceDataManager.PropertyValue::getState);
        }
        return propertyValueMono
            .map(DeviceDataManager.PropertyValue::getValue);

    }

    Mono<DeviceDataManager.PropertyValue> getPropertyValue(String deviceId, String property) {
        return deviceDataManager.getLastProperty(deviceId, property);
    }
}
