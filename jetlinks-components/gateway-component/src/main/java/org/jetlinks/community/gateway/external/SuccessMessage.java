package org.jetlinks.community.gateway.external;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SuccessMessage implements Message {

    private String requestId;

    private String topic;

    private Object payload;


    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String getMessage() {
        return null;
    }
}
