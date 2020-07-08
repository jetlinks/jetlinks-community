package org.jetlinks.community.gateway.external.socket;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MessagingRequest {

    private String id;

    private Type type;

    private String topic;

    private Map<String,Object> parameter;


    public enum Type{
        pub,sub,unsub,ping
    }
}
