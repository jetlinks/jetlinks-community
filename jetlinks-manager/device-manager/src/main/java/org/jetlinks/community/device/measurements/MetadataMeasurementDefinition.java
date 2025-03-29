package org.jetlinks.community.device.measurements;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.community.dashboard.MeasurementDefinition;

@AllArgsConstructor(staticName = "of")
@Generated
public class MetadataMeasurementDefinition implements MeasurementDefinition {

    Metadata metadata;

    @Override
    @Generated
    public String getId() {
        return metadata.getId();
    }

    @Override
    @Generated
    public String getName() {
        return metadata.getName();
    }
}
