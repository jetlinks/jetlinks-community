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
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @author wanghzeng
 * @since 1.0
 **/
@Setter
public class MeterRegistryManager {


    private Map<String, MeterRegistry> meterRegistryMap = new ConcurrentHashMap<>();

    private final List<MeterRegistrySupplier> suppliers;

    private final List<MeterRegistryCustomizer> customizers;

    public MeterRegistryManager(List<MeterRegistrySupplier> suppliers,
                                List<MeterRegistryCustomizer> customizers) {
        this.suppliers = suppliers == null ? Collections.emptyList() : suppliers;
        this.customizers = customizers == null ? Collections.emptyList() : customizers;
    }

    public void shutdown() {
        meterRegistryMap
            .values()
            .forEach(MeterRegistry::close);
    }

    private MeterRegistry createMeterRegistry(String metric, Map<String, DataType> tagDefine) {
        Map<String, DataType> tags = new HashMap<>(tagDefine);
        MeterRegistrySettings settings = tags::put;
        customizers.forEach(customizer -> customizer.custom(metric, settings));

        if (suppliers.size() == 1) {
            return suppliers.get(0).getMeterRegistry(metric, tags);
        }

        return new CompositeMeterRegistry(Clock.SYSTEM, suppliers
            .stream()
            .map(supplier -> supplier.getMeterRegistry(metric, tags))
            .collect(Collectors.toList()));
    }

    public MeterRegistry getMeterRegister(String metric, String... tagKeys) {

        return meterRegistryMap.computeIfAbsent(metric, _metric -> {
            if (tagKeys.length == 0) {
                return createMeterRegistry(metric, Collections.emptyMap());
            }
            return createMeterRegistry(metric, Arrays
                .stream(tagKeys)
                .collect(Collectors.toMap(Function.identity(), key -> StringType.GLOBAL)));
        });
    }

    @Deprecated
    public MeterRegistry getMeterRegister(String metric, Map<String, DataType> tagDefine) {
        return meterRegistryMap.computeIfAbsent(metric, _metric -> createMeterRegistry(_metric, tagDefine));
    }


}
