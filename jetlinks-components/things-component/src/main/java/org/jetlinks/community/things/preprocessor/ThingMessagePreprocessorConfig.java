package org.jetlinks.community.things.preprocessor;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.things.MetadataId;

import java.util.Map;

@Getter
@Setter
public class ThingMessagePreprocessorConfig {

    private String thingType;
    private String templateId;
    private String thingId;
    private MetadataId metadataId;

    private Map<String, Object> configuration;

}
