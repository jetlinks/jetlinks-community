package org.jetlinks.community.timescaledb.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.FeatureType;
import org.hswebframework.ezorm.core.meta.Feature;
import org.jetlinks.community.timescaledb.TimescaleDBProperties;

@AllArgsConstructor(staticName = "of")
@Getter
public class TimescaleDBPropertiesFeature implements Feature, FeatureType {

    public static final FeatureId<TimescaleDBPropertiesFeature> ID = FeatureId.of("TimescaleDBPropertiesFeature");

    private final TimescaleDBProperties properties;

    @Override
    public String getId() {
        return ID.getId();
    }

    @Override
    public String getName() {
        return "TimescaleDBPropertiesFeature";
    }

    @Override
    public FeatureType getType() {
        return this;
    }
}
