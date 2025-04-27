package org.jetlinks.community.timescaledb.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.FeatureType;
import org.hswebframework.ezorm.core.meta.Feature;
import org.jetlinks.community.Interval;

@Getter
@AllArgsConstructor
public class CreateHypertable implements Feature, FeatureType {

    public static final FeatureId<CreateHypertable> ID = FeatureId.of("CreateHypertable");

    private final String column;

    private final Interval chunkTimeInterval;

    @Override
    public String getId() {
        return ID.getId();
    }

    @Override
    public String getName() {
        return "CreateHypertable";
    }

    @Override
    public FeatureType getType() {
        return this;
    }
}
