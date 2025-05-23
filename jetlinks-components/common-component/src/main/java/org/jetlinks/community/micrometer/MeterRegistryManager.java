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
package org.jetlinks.community.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @author wanghzeng
 * @since 1.0
 **/
@Component
@Setter
public class MeterRegistryManager {

    private Map<String, MeterRegistry> meterRegistryMap = new ConcurrentHashMap<>();

    @Autowired
    private List<MeterRegistrySupplier> suppliers;

    private MeterRegistry createMeterRegistry(String metric, String... tagKeys) {
        return new CompositeMeterRegistry(Clock.SYSTEM,
            suppliers.stream()
                .map(supplier -> supplier.getMeterRegistry(metric, tagKeys))
                .collect(Collectors.toList()));
    }

    public MeterRegistry getMeterRegister(String metric, String... tagKeys) {
        return meterRegistryMap.computeIfAbsent(metric, _metric -> createMeterRegistry(_metric, tagKeys));
    }

}
