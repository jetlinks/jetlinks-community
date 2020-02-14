package org.jetlinks.community.device.measurements;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.community.dashboard.MeasurementDefinition;

@AllArgsConstructor(staticName = "of")
public class MetadataMeasurementDefinition implements MeasurementDefinition {

    Metadata metadata;

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }
}
