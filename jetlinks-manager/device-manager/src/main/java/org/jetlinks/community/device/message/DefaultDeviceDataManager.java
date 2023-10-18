package org.jetlinks.community.device.message;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * 设备最近数据管理器，用于获取设备最近的属性数据等.
 *
 * @author zhouhao
 * @since 1.9
 */
//@Component
    @Deprecated
@AllArgsConstructor
public class DefaultDeviceDataManager implements DeviceDataManager {

    private final DeviceRegistry registry;

    private final DeviceDataService dataService;

    private final EventBus eventBus;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final Map<String, DevicePropertyRef> localCache = newCache();

    static <K, V> Map<K, V> newCache() {
        return Caffeine
            .newBuilder()
            //10分钟没有访问则过期
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, removalCause) -> {
                if (value instanceof Disposable) {
                    ((Disposable) value).dispose();
                }
            })
            .<K, V>build()
            .asMap();
    }

    @Override
    public Mono<PropertyValue> getLastProperty(@Nonnull String deviceId, @Nonnull String propertyId) {
        return localCache
            .computeIfAbsent(deviceId, id -> new DevicePropertyRef(id, eventBus, dataService))
            .getLastProperty(propertyId, System.currentTimeMillis());
    }

    @Override
    public Mono<PropertyValue> getLastProperty(@Nonnull String deviceId, @Nonnull String propertyId, long baseTime) {
        return localCache
            .computeIfAbsent(deviceId, id -> new DevicePropertyRef(id, eventBus, dataService))
            .getLastProperty(propertyId, baseTime);
    }

    @Override
    public Mono<Long> getLastPropertyTime(@Nonnull String deviceId, long baseTime) {
        return localCache
            .computeIfAbsent(deviceId, id -> new DevicePropertyRef(id, eventBus, dataService))
            .getRecentPropertyTime(baseTime);
    }

    @Override
    public Mono<Long> getFirstPropertyTime(@Nonnull String deviceId) {
        return registry
            .getDevice(deviceId)
            .flatMap(device -> device.getSelfConfig(DeviceConfigKey.firstPropertyTime));
    }

    @Override
    public Mono<PropertyValue> getFirstProperty(@Nonnull String deviceId, @Nonnull String propertyId) {
        return localCache
            .computeIfAbsent(deviceId, id -> new DevicePropertyRef(id, eventBus, dataService))
            .getFirstProperty(propertyId);
    }

    @Override
    public Flux<TagValue> getTags(@Nonnull String deviceId, String... tagIdList) {
        Assert.hasText(deviceId, "deviceId must not be empty");

        return registry
            .getDevice(deviceId)
            .flatMap(DeviceOperator::getMetadata)
            .flatMapMany(metadata -> tagRepository
                .createQuery()
                .where(DeviceTagEntity::getDeviceId, deviceId)
                .when(tagIdList != null && tagIdList.length > 0, q -> q.in(DeviceTagEntity::getKey, (Object[]) tagIdList))
                .fetch()
                .map(tag -> DefaultTagValue.of(tag.getKey(), tag.getValue(), metadata.getTagOrNull(tag.getKey()))));

    }

    @Getter
    public static class DefaultTagValue implements TagValue {
        private String tagId;
        private Object value;

        public static DefaultTagValue of(String id, String value, PropertyMetadata metadata) {
            DefaultTagValue tagValue = new DefaultTagValue();
            tagValue.tagId = id;

            if (metadata != null && metadata.getValueType() instanceof Converter) {
                tagValue.value = ((Converter<?>) metadata.getValueType()).convert(value);
            } else {
                tagValue.value = value;
            }
            return tagValue;
        }
    }

    //更新首次上报属性的时间
    @Subscribe(topics = {
        "/device/*/*/message/property/report",
        "/device/*/*/message/property/read,write/reply"
    }, features = Subscription.Feature.local)
    public Mono<Void> upgradeDeviceFirstPropertyTime(DeviceMessage message) {
        return registry
            .getDevice(message.getDeviceId())
            .flatMap(device -> device
                .getSelfConfig(DeviceConfigKey.firstPropertyTime)
                //没有首次上报时间就更新,设备注销后,首次上报属性时间也将失效.
                .switchIfEmpty(device
                                   .setConfig(DeviceConfigKey.firstPropertyTime, message.getTimestamp())
                                   .thenReturn(message.getTimestamp())
                ))
            .then();
    }


    private static class PropertyRef implements PropertyValue {
        @Getter
        private volatile Object value;
        @Getter
        private volatile long timestamp;
        @Getter
        private volatile String state;

        //上一个
        private transient PropertyRef pre;
        //第一个
        private transient PropertyRef first;

        PropertyRef setValue(Object value, long ts, String state) {
            //只处理比较新的数据
            if (this.value == null || this.value == NULL || ts >= this.timestamp) {
                if (pre == null) {
                    pre = new PropertyRef();
                }
                pre.value = this.value;
                pre.timestamp = this.timestamp;
                pre.state = this.state;
                this.value = value;
                this.timestamp = ts;
                this.state = state;
            }
            return this;
        }

        void setNull() {
            if (value == null) {
                value = NULL;
            }
        }

        PropertyRef setFirst(Object value, long ts) {
            if (first == null) {
                first = new PropertyRef();
            }
            if (first.value == null || first.value == NULL || ts <= first.timestamp) {
                first.value = value;
                first.timestamp = ts;
            }
            return first;
        }

        PropertyRef setFirstNull() {
            if (first == null) {
                first = new PropertyRef();
                first.value = NULL;
                first.state = null;
            }
            return first;
        }

        PropertyRef copy() {
            PropertyRef ref = new PropertyRef();
            ref.state = state;
            ref.timestamp = timestamp;
            ref.value = value;
            return ref;
        }
    }

    static class DevicePropertyRef implements Disposable {
        Disposable disposable;
        Map<String, PropertyRef> refs = newCache();
        String deviceId;
        DeviceDataService dataService;
        private long lastPropertyTime;
        private long propertyTime;

        public DevicePropertyRef(String deviceId, EventBus eventBus, DeviceDataService dataService) {
            this.dataService = dataService;
            this.deviceId = deviceId;
            //订阅消息总线的数据来实时更新
            Subscription subscription = Subscription
                .builder()
                .subscriberId("device_recent_property:" + deviceId)
                .topics("/device/*/" + deviceId + "/message/property/report",
                        "/device/*/" + deviceId + "/message/property/read,write/reply")
                .broker()
                .local()
                .build();

            disposable = eventBus
                .subscribe(subscription, DeviceMessage.class)
                .subscribe(this::upgrade);
        }

        private void upgrade(DeviceMessage message) {
            Map<String, Object> properties = DeviceMessageUtils
                .tryGetProperties(message)
                .orElseGet(Collections::emptyMap);

            Map<String, Long> propertyTime = DeviceMessageUtils
                .tryGetPropertySourceTimes(message)
                .orElseGet(Collections::emptyMap);

            Map<String, String> propertyState = DeviceMessageUtils
                .tryGetPropertyStates(message)
                .orElse(Collections.emptyMap());

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                PropertyRef ref = refs.get(entry.getKey());
                //只更新有人读取过的属性,节省内存
                if (null != ref) {
                    ref.setValue(entry.getValue(),
                                 propertyTime.getOrDefault(entry.getKey(), message.getTimestamp()),
                                 propertyState.getOrDefault(entry.getKey(), ref.state));
                }
            }
            updatePropertyTime(message.getTimestamp());
        }

        private long updatePropertyTime(long timestamp) {
            if (propertyTime <= timestamp) {
                this.lastPropertyTime = propertyTime;
                this.propertyTime = timestamp;
            }
            return propertyTime;
        }

        public Mono<PropertyValue> getFirstProperty(String property) {
            PropertyRef ref = refs.computeIfAbsent(property, ignore -> new PropertyRef());
            if (ref.first != null && ref.first.getValue() != null) {
                if (ref.first.getValue() == NULL) {
                    return Mono.empty();
                }
                return Mono.just(ref.first);
            }
            return dataService
                //直接查询设备数据,可能会有延迟?
                .queryProperty(deviceId,
                               QueryParamEntity
                                   .newQuery()
                                   .orderByAsc("timestamp")
                                   .getParam()
                )
                .take(1)
                .singleOrEmpty()
                .<PropertyValue>map(prop -> ref.setFirst(prop.getValue(), prop.getTimestamp()))
                .switchIfEmpty(Mono.fromRunnable(ref::setFirstNull))
                ;
        }

        public Mono<Long> getRecentPropertyTime(long baseTime) {
            if (propertyTime == -1) {
                return Mono.empty();
            }
            if (propertyTime > 0 && propertyTime < baseTime) {
                return Mono.just(propertyTime);
            }
            if (lastPropertyTime > 0 && lastPropertyTime < baseTime) {
                return Mono.just(lastPropertyTime);
            }
            //查询最新属性
            return QueryParamEntity
                .newQuery()
                .orderByDesc("timestamp")
                .when(propertyTime > 0, q -> q.lt("timestamp", baseTime))
                .doPaging(0, 1)
                .execute(param -> dataService.queryProperty(deviceId, param))
                .take(1)
                .singleOrEmpty()
                .map(val -> {
                    if (propertyTime <= 0) {
                        updatePropertyTime(val.getTimestamp());
                    }
                    return val.getTimestamp();
                })
                .switchIfEmpty(Mono
                                   .fromRunnable(() -> {
                                       if (this.propertyTime == 0) {
                                           propertyTime = -1;
                                       }
                                   }));
        }

        public Mono<PropertyValue> getLastProperty(String key, long baseTime) {
            PropertyRef ref = refs.computeIfAbsent(key, ignore -> new PropertyRef());
            Object val = ref.getValue();
            if (val == NULL) {
                return Mono.empty();
            }
            Function<Mono<DeviceProperty>, Mono<PropertyValue>> resultHandler;
            if (val != null) {
                //本地缓存
                if (ref.timestamp < baseTime) {
                    return Mono.just(ref.copy());
                }
                if (ref.pre != null && ref.pre.timestamp < baseTime && ref.pre.value != NULL) {
                    return Mono.just(ref.pre.copy());
                }
                //获取当前数据之前的数据
                resultHandler = prop -> prop
                    .map(deviceProperty -> new PropertyRef()
                        .setValue(
                            deviceProperty.getValue(),
                            deviceProperty.getTimestamp(),
                            deviceProperty.getState()
                        ));
            } else {
                //当前没有任何数据,则进行查询
                resultHandler = prop -> prop
                    .<PropertyValue>map(deviceProperty -> ref.setValue(
                        deviceProperty.getValue(),
                        deviceProperty.getTimestamp(),
                        deviceProperty.getState())
                    )
                    .switchIfEmpty(Mono.fromRunnable(ref::setNull));
            }

            return QueryParamEntity
                .newQuery()
                .orderByDesc("timestamp")
                .doPaging(0, 1)
                .where()
                .lt("timestamp", baseTime)
                .execute(param -> dataService.queryProperty(deviceId, param, key))
                .take(1)
                .singleOrEmpty()
                .as(resultHandler)
                ;
        }

        @Override
        public void dispose() {
            disposable.dispose();
        }
    }

    static Object NULL = new Object();
}
