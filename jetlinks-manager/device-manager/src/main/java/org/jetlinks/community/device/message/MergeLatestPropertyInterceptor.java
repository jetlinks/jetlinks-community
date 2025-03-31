package org.jetlinks.community.device.message;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.property.PropertyMessage;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;

/**
 * 合并最新的数据到消息中
 *
 * @author zhouhao
 * @since 1.12
 */
@Component
@AllArgsConstructor
@Slf4j
public class MergeLatestPropertyInterceptor implements DeviceMessagePublishInterceptor {

    private final DeviceDataManager dataManager;

    @Override
    public boolean isSupport(DeviceMessage message) {
        return message.getHeaderOrElse(Headers.mergeLatest, () -> false);
    }

    @Override
    public Mono<DeviceMessage> beforePublish(DeviceOperator device, DeviceMessage msg) {
        if (!(msg instanceof PropertyMessage)) {
            return Mono.just(msg);
        }
        PropertyMessage message = ((PropertyMessage) msg);
        return Mono
            .zip(
                Mono.justOrEmpty(DeviceMessageUtils.tryGetProperties(msg)),
                device
                    .getMetadata()
                    .flatMapIterable(DeviceMetadata::getProperties)
                    .flatMap(metadata -> {
                        if (message.getProperty(metadata.getId()).isPresent()) {
                            return Mono.empty();
                        }
                        return dataManager
                            .getLastProperty(device.getDeviceId(),
                                             metadata.getId(),
                                             message.getTimestamp())
                            .map(val -> Tuples.of(metadata.getId(), val));
                    })
                    .collectMap(Tuple2::getT1, Tuple2::getT2),
                (newest, latestData) -> {
                    if (latestData.isEmpty()) {
                        return message;
                    }
                    message.properties(new CompositeMap<>(newest, Maps.transformValues(latestData, DeviceDataManager.PropertyValue::getValue)));
                    //时间
                    Map<String, Long> sourceTime = message.getPropertySourceTimes();
                    if (sourceTime == null) {
                        sourceTime = Maps.transformValues(latestData, DeviceDataManager.PropertyValue::getTimestamp);
                    } else {
                        sourceTime = new CompositeMap<>(sourceTime, Maps.transformValues(latestData, DeviceDataManager.PropertyValue::getTimestamp));
                    }
                    message.propertySourceTimes(sourceTime);

                    //状态
                    Map<String, String> sourceState = message.getPropertyStates();
                    if (sourceState == null) {
                        sourceState = Maps.transformValues(latestData, DeviceDataManager.PropertyValue::getState);
                    } else {
                        sourceState = new CompositeMap<>(sourceState, Maps.transformValues(latestData, DeviceDataManager.PropertyValue::getState));
                    }
                    message.propertyStates(sourceState);

                    return message;
                }
            )
            .onErrorResume((err) -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            })
            .thenReturn(msg);
    }
}
