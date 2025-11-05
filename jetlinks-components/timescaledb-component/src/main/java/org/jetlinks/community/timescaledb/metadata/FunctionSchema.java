package org.jetlinks.community.timescaledb.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.FeatureId;
import org.hswebframework.ezorm.core.FeatureType;
import org.hswebframework.ezorm.core.meta.Feature;

@AllArgsConstructor(staticName = "of")
@Getter
public class FunctionSchema implements Feature, FeatureType {

    public static final FeatureId<FunctionSchema> ID = FeatureId.of("FunctionSchema");

    private final String functionSchema;

    @Override
    public String getId() {
        return ID.getId();
    }

    @Override
    public String getName() {
        return "FunctionSchema";
    }

    @Override
    public FeatureType getType() {
        return this;
    }
}
