package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DeviceGatewayProperties  implements ValueObject {

    private String id;

    private String provider;

    private String networkId;

    private Map<String,Object> configuration=new HashMap<>();

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
