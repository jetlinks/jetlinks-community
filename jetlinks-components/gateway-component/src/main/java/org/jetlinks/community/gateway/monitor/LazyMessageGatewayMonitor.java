package org.jetlinks.community.gateway.monitor;

import java.util.function.Supplier;

class LazyMessageGatewayMonitor implements MessageGatewayMonitor {

    private volatile MessageGatewayMonitor target;

    private Supplier<MessageGatewayMonitor> monitorSupplier;

    public LazyMessageGatewayMonitor(Supplier<MessageGatewayMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public MessageGatewayMonitor getTarget() {
        if (target == null || target instanceof NoneMessageGatewayMonitor) {
            target = monitorSupplier.get();
        }
        return target;
    }

    @Override
    public void totalSession(long sessionNumber) {
        getTarget().totalSession(sessionNumber);
    }

    @Override
    public void acceptedSession() {
        getTarget().acceptedSession();
    }

    @Override
    public void closedSession() {
        getTarget().closedSession();
    }

    @Override
    public void subscribed() {
        getTarget().subscribed();
    }

    @Override
    public void unsubscribed() {
        getTarget().unsubscribed();
    }

    @Override
    public void dispatched(String connector) {
        getTarget().dispatched(connector);
    }

    @Override
    public void acceptMessage() {
        getTarget().acceptMessage();
    }

    @Override
    public void dispatchError(String connector, String sessionId, Throwable error) {
        getTarget().dispatchError(connector, sessionId, error);
    }
}
