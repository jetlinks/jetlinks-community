package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.NonNull;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.core.things.ThingsDataManager;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ThingDeviceDataManager implements DeviceDataManager {

    private final DeviceRegistry registry;
    private final ThingsDataManager thingsDataManager;
    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    @Override
    public Mono<PropertyValue> getLastProperty(@NonNull String deviceId, @NonNull String propertyId) {
        return getLastProperty(deviceId, propertyId, System.currentTimeMillis());
    }

    @Override
    public Mono<PropertyValue> getLastProperty(@NonNull String deviceId, @NonNull String propertyId, long baseTime) {
        return thingsDataManager
            .getLastProperty(DeviceThingType.device, deviceId, propertyId, baseTime)
            .map(PropertyValueInfo::of);
    }

    @Override
    public Mono<PropertyValue> getFirstProperty(@NonNull String deviceId, @NonNull String propertyId) {
        return thingsDataManager
            .getFirstProperty(DeviceThingType.device, deviceId, propertyId)
            .map(PropertyValueInfo::of);
    }

    @Override
    public Mono<Long> getLastPropertyTime(@NonNull String deviceId, long baseTime) {
        return thingsDataManager
            .getLastPropertyTime(DeviceThingType.device, deviceId, baseTime);
    }

    @Override
    public Mono<Long> getFirstPropertyTime(@NonNull String deviceId) {
        return thingsDataManager
            .getFirstPropertyTime(DeviceThingType.device, deviceId);
    }

    @Override
    public Flux<TagValue> getTags(@NonNull String deviceId, String... tagIdList) {
        Assert.hasText(deviceId, "deviceId must not be empty");

        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> tagRepository
                .createQuery()
                .where(DeviceTagEntity::getDeviceId, deviceId)
                .when(tagIdList != null && tagIdList.length > 0, q -> q.in(DeviceTagEntity::getKey, (Object[]) tagIdList))
                .fetch()
                .map(tag -> TagValueInfo.of(tag.getKey(), tag.getValue(), metadata.getTagOrNull(tag.getKey()))));
    }

    @Getter
    public static class TagValueInfo implements TagValue {
        @Generated
        private String tagId;
        @Generated
        private Object value;

        public static TagValueInfo of(String id, String value, PropertyMetadata metadata) {
            TagValueInfo tagValue = new TagValueInfo();
            tagValue.tagId = id;

            if (metadata != null && metadata.getValueType() instanceof Converter) {
                tagValue.value = ((Converter<?>) metadata.getValueType()).convert(value);
            } else {
                tagValue.value = value;
            }
            return tagValue;
        }
    }

    @AllArgsConstructor(staticName = "of")
    static class PropertyValueInfo implements PropertyValue {
        private final ThingProperty property;

        @Override
        public long getTimestamp() {
            return property.getTimestamp();
        }

        @Override
        public Object getValue() {
            return property.getValue();
        }

        @Override
        public String getState() {
            return property.getState();
        }
    }
}
