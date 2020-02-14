package org.jetlinks.community.gateway.monitor;

public interface MessageGatewayMonitor {

    void totalSession(long sessionNumber);

    void acceptedSession();

    void closedSession();

    void subscribed();

    void unsubscribed();

    void dispatched(String connector);

    void acceptMessage();

    void dispatchError(String connector, String sessionId, Throwable error);
}
