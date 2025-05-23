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

import org.jetlinks.core.Value;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.reactor.ql.supports.map.FunctionMapFeature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 在reactorQL中获取设备配置
 * <pre>{@code
 * select device.config(deviceId,'password') pwd from ...
 *
 * select * from ... where device.config(deviceId,'password') = 'xxx'
 * }</pre>
 *
 * @author zhouhao
 * @since 1.13
 */
@Component
public class DeviceConfigFunction extends FunctionMapFeature {
    public DeviceConfigFunction(DeviceRegistry registry) {
        super("device.config", 2, 2, flux -> flux
            .collectList()
            .flatMap(args -> {
                if (args.size() != 2) {
                    return Mono.empty();
                }
                String deviceId = String.valueOf(args.get(0));
                String configKey = String.valueOf(args.get(1));

                return registry
                    .getDevice(deviceId)
                    .flatMap(device -> device
                        .getConfig(configKey)
                        .map(Value::get));
            }));
    }
}
