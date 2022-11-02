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
@Component
@Setter
public class MeterRegistryManager {

    private Map<String, MeterRegistry> meterRegistryMap = new ConcurrentHashMap<>();

    private final List<MeterRegistrySupplier> suppliers;


    public MeterRegistryManager(@Autowired(required = false) List<MeterRegistrySupplier> suppliers) {
        this.suppliers = suppliers == null ? new ArrayList<>() : suppliers;
    }


    private MeterRegistry createMeterRegistry(String metric, Map<String, DataType> tagDefine) {
        Map<String, DataType> tags = new HashMap<>(tagDefine);
        MeterRegistrySettings settings = tags::put;
        return new CompositeMeterRegistry(Clock.SYSTEM, suppliers
            .stream()
            .map(supplier -> supplier.getMeterRegistry(metric))
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

}
