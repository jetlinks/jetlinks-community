package org.jetlinks.community.gateway.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SimpleMessage implements Message {

    private String requestId;

    private String topic;

    private Object payload;

    private Type type;

    private String message;

}
