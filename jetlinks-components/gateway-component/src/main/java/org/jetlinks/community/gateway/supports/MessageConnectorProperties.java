package org.jetlinks.community.gateway.supports;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class MessageConnectorProperties {

    private String id;

    private String provider;

    private Map<String,Object> configuration=new HashMap<>();


}
