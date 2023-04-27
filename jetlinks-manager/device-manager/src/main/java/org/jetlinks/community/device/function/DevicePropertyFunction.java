package org.jetlinks.community.device.function;

import org.jetlinks.core.message.DeviceDataManager;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 在reactorQL中获取设备最新属性
 * <pre>{@code
 * select device.property.recent(deviceId,'temperature',timestamp) recent from ...
 *
 * select * from ... where device.property.recent(deviceId,'temperature',timestamp)  = 'xxx'
 * }</pre>
 *
 * @author zhouhao
 * @since 2.0
 */
@Component
public class DevicePropertyFunction extends FunctionMapFeature {
    public DevicePropertyFunction(DeviceDataManager dataManager) {
        super("device.property.recent", 3, 2, flux -> flux
            .collectList()
            .flatMap(args -> {
                if (args.size() < 2) {
                    return Mono.empty();
                }
                String deviceId = String.valueOf(args.get(0));
                String property = String.valueOf(args.get(1));
                long timestamp = args.size() > 2
                    ? CastUtils.castNumber(args.get(2))
                               .longValue()
                    : System.currentTimeMillis();

                return dataManager
                    .getLastProperty(deviceId, property, timestamp)
                    .map(DeviceDataManager.PropertyValue::getValue);
            }));
    }
}
