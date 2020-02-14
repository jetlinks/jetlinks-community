package org.jetlinks.community.gateway.monitor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

class CompositeDeviceGatewayMonitor implements DeviceGatewayMonitor {

    private List<DeviceGatewayMonitor> monitors = new ArrayList<>();

    public CompositeDeviceGatewayMonitor add(DeviceGatewayMonitor... monitors) {
        return add(Arrays.asList(monitors));
    }

    public CompositeDeviceGatewayMonitor add(Collection<DeviceGatewayMonitor> monitors) {
        this.monitors.addAll(monitors);
        return this;
    }

    protected void doWith(Consumer<DeviceGatewayMonitor> monitorConsumer) {
        monitors.forEach(monitorConsumer);
    }


    @Override
    public void totalConnection(long total) {
        doWith(monitor -> monitor.totalConnection(total));
    }

    @Override
    public void connected() {
        doWith(DeviceGatewayMonitor::connected);
    }

    @Override
    public void rejected() {
        doWith(DeviceGatewayMonitor::rejected);
    }

    @Override
    public void disconnected() {
        doWith(DeviceGatewayMonitor::disconnected);
    }

    @Override
    public void receivedMessage() {
        doWith(DeviceGatewayMonitor::receivedMessage);
    }

    @Override
    public void sentMessage() {
        doWith(DeviceGatewayMonitor::sentMessage);
    }
}
