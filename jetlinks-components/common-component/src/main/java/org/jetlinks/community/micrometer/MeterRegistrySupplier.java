package org.jetlinks.community.micrometer;

import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistrySupplier {

    MeterRegistry getMeterRegistry(String metric, String... tagKeys);

}
