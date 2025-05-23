/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
