package org.jetlinks.community.gateway.external;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ErrorMessage implements Message {

    private String requestId;

    private String topic;

    @Getter
    private String message;

    @Override
    public Object getPayload() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

}
