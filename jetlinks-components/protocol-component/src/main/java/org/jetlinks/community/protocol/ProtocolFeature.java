package org.jetlinks.community.protocol;

import lombok.*;
import org.jetlinks.core.metadata.Feature;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ProtocolFeature {
    private String id;
    private String name;

    @Generated
    public static ProtocolFeature of(Feature feature) {
        return new ProtocolFeature(feature.getId(), feature.getName());
    }
}
