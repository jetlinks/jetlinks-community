package org.jetlinks.community.gateway.external;

public interface Message {

    String getRequestId();

    String getTopic();

    Object getPayload();

    boolean isSuccess();

    String getMessage();

    static Message error(String id, String topic, String message) {
        return new ErrorMessage(id, topic, message);
    }

    static Message success(String id, String topic, Object payload) {
        return new SuccessMessage(id, topic, payload);
    }

}
