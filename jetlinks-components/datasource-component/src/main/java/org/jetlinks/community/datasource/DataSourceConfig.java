package org.jetlinks.community.datasource;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.util.Map;

@Getter
@Setter
@Generated
public class DataSourceConfig implements ValueObject {
    private String id;
    private String typeId;
    private Map<String,Object> configuration;

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
