package org.jetlinks.community.things.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.community.things.data.operations.*;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

public interface ThingsDataRepositoryStrategy extends Ordered {

    String getId();

    String getName();

    SaveOperations opsForSave(OperationsContext context);

    ThingOperations opsForThing(String thingType, String templateId, String thingId, OperationsContext context);

    TemplateOperations opsForTemplate(String thingType, String templateId, OperationsContext context);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    default Flux<Feature> getFeatures() {
        return Flux.empty();
    }

    @EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
    @Getter
    @AllArgsConstructor
    class OperationsContext {
        public static final OperationsContext DEFAULT = new OperationsContext(MetricBuilder.DEFAULT, new DataSettings());

        private final MetricBuilder metricBuilder;
        private final DataSettings settings;

        public OperationsContext settings(DataSettings settings) {
            return new OperationsContext(metricBuilder, settings);
        }

        public OperationsContext metricBuilder(MetricBuilder metricBuilder) {
            return new OperationsContext(metricBuilder, settings);
        }

    }
}
