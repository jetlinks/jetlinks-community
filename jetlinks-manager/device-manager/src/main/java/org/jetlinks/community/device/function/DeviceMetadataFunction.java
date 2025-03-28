package org.jetlinks.community.device.function;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;

@Component
public class DeviceMetadataFunction extends FunctionMapFeature {
    public DeviceMetadataFunction(DeviceRegistry registry) {
        super("device.metadata", 1, 1, args -> args
            .collectList()
            .flatMap(arg -> {
                String deviceId = String.valueOf(arg.get(0));
                return registry
                    .getDevice(deviceId)
                    .flatMap(DeviceOperator::getMetadata)
                    .map(ThingMetadata::toJson);
            }));
    }
}
