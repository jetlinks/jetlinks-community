package org.jetlinks.community.device.function;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DeviceMetadataFunctionFunction extends FunctionMapFeature {
    public DeviceMetadataFunctionFunction(DeviceRegistry registry) {
        super("device.metadata.func", 2, 2, args -> args
            .collectList()
            .flatMap(arg -> {
                String deviceId = String.valueOf(arg.get(0));
                String function = String.valueOf(arg.get(1));
                return registry.getDevice(deviceId)
                               .flatMap(DeviceOperator::getMetadata)
                               .flatMap(metadata -> Mono
                                   .justOrEmpty(metadata.getFunctionOrNull(function))
                                   .map(Jsonable::toJson)
                               );
            }));
    }
}
