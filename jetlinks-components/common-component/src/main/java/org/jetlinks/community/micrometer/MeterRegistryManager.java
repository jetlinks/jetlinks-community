package org.jetlinks.community.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
