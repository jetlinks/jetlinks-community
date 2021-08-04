package org.jetlinks.community.gateway.external;

public interface Message {

    String getRequestId();

    String getTopic();

    Object getPayload();

    String getMessage();

    Type getType();

    static Message authError() {

        return new SimpleMessage(null, null, null, Type.authError, "认证失败");
    }

    static Message error(String id, String topic, String message) {

        return new SimpleMessage(id, topic, null, Type.error, message);
    }

    static Message error(String id, String topic, Throwable message) {

        return new SimpleMessage(id, topic, null, Type.error, message.getMessage() == null ? message.getClass().getSimpleName() : message.getMessage());
    }

    static Message success(String id, String topic, Object payload) {
        return new SimpleMessage(id, topic, payload, Type.result, null);
    }

    static Message complete(String id) {
        return new SimpleMessage(id, null, null, Type.complete, null);
    }

    static Message pong(String id) {
        return new SimpleMessage(id, null, null, Type.pong, null);
    }

    enum Type {
        authError,
        result,
        error,
        complete,
        ping,
        pong
    }
}
