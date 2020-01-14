package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DeviceGatewayProperties {

    private String id;

    private String provider;

    private String networkId;

    private Map<String,Object> configuration=new HashMap<>();


}
