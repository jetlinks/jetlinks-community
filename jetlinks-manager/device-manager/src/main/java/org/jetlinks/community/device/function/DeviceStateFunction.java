package org.jetlinks.community.device.function;

import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;

/**
 * 在reactorQL中获取设备当前状态
 *
 * <ul>
 *     <li>device.state('test1'): 获取设备id为test1的当前状态</li>
 *     <li>device.state('test1',true): 获取设备id为test1的真实状态</li>
 * </ul>
 * <pre>{@code
 * select device.state(deviceId) state from ...
 *
 * select * from ... where device.state(deviceId) = 1
 * }</pre>
 *
 * @author zhouhao
 * @see DeviceState
 * @since 2.2
 */
@Component
public class DeviceStateFunction extends FunctionMapFeature {
    public DeviceStateFunction(DeviceRegistry registry) {
        super("device.state", 2, 1, flux -> flux
            .collectList()
            .flatMap(args -> {
                String deviceId = String.valueOf(args.get(0));
                boolean check = args.size() == 2 && CastUtils.castBoolean(args.get(1));

                return registry
                    .getDevice(deviceId)
                    .flatMap(device -> check ? device.checkState() : device.getState())
                    .defaultIfEmpty(DeviceState.noActive);
            }));
    }
}
