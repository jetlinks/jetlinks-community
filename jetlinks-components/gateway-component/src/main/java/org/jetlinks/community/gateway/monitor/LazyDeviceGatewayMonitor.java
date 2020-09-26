package org.jetlinks.community.gateway.monitor;

import java.util.function.Supplier;

class LazyDeviceGatewayMonitor implements DeviceGatewayMonitor {

    private volatile DeviceGatewayMonitor target;

    private Supplier<DeviceGatewayMonitor> monitorSupplier;

    public LazyDeviceGatewayMonitor(Supplier<DeviceGatewayMonitor> monitorSupplier) {
        this.monitorSupplier = monitorSupplier;
    }

    public DeviceGatewayMonitor getTarget() {
        if (target == null) {
            target = monitorSupplier.get();
        }
        return target;
    }


    @Override
    public void totalConnection(long total) {
        getTarget().totalConnection(total);
    }

    @Override
    public void connected() {
        getTarget().connected();
    }

    @Override
    public void rejected() {
        getTarget().rejected();
    }

    @Override
    public void disconnected() {
        getTarget().disconnected();
    }

    @Override
    public void receivedMessage() {
        getTarget().receivedMessage();
    }

    @Override
    public void sentMessage() {
        getTarget().sentMessage();
    }
}
