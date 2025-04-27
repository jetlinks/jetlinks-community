package org.jetlinks.community.timescaledb.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.FeatureType;
import org.hswebframework.ezorm.core.meta.Feature;
import org.jetlinks.community.Interval;

@Getter
@AllArgsConstructor
public class CreateRetentionPolicy implements Feature, FeatureType {

    public static final FeatureId<CreateRetentionPolicy> ID = FeatureId.of("CreateRetentionPolicy");

    private final Interval interval;

    @Override
    public String getId() {
        return ID.getId();
    }

    @Override
    public String getName() {
        return "CreateRetentionPolicy";
    }

    @Override
    public FeatureType getType() {
        return this;
    }
}
