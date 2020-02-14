package org.jetlinks.community.gateway.monitor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GatewayMonitors {

    private static final List<MessageGatewayMonitorSupplier> suppliers = new CopyOnWriteArrayList<>();

    private static final List<DeviceGatewayMonitorSupplier> deviceGatewayMonitorSuppliers = new CopyOnWriteArrayList<>();

    static final NoneMessageGatewayMonitor none = new NoneMessageGatewayMonitor();

    static final NoneDeviceGatewayMonitor nonDevice = new NoneDeviceGatewayMonitor();


    static {

    }

    public static void register(MessageGatewayMonitorSupplier supplier) {
        suppliers.add(supplier);
    }


    public static void register(DeviceGatewayMonitorSupplier supplier) {
        deviceGatewayMonitorSuppliers.add(supplier);
    }

    private static MessageGatewayMonitor doGetMessageGatewayMonitor(String id, String... tags) {
        List<MessageGatewayMonitor> all = suppliers.stream()
            .map(supplier -> supplier.getMessageGatewayMonitor(id, tags))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (all.isEmpty()) {
            return none;
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        CompositeMessageGatewayMonitor monitor = new CompositeMessageGatewayMonitor();
        monitor.add(all);
        return monitor;
    }

    private static DeviceGatewayMonitor doGetDeviceGatewayMonitor(String id, String... tags) {
        List<DeviceGatewayMonitor> all = deviceGatewayMonitorSuppliers.stream()
            .map(supplier -> supplier.getDeviceGatewayMonitor(id, tags))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (all.isEmpty()) {
            return nonDevice;
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        CompositeDeviceGatewayMonitor monitor = new CompositeDeviceGatewayMonitor();
        monitor.add(all);
        return monitor;
    }

    public static MessageGatewayMonitor getMessageGatewayMonitor(String id, String... tags) {
        return new LazyMessageGatewayMonitor(() -> doGetMessageGatewayMonitor(id, tags));
    }

    public static DeviceGatewayMonitor getDeviceGatewayMonitor(String id, String... tags) {
        return new LazyDeviceGatewayMonitor(() -> doGetDeviceGatewayMonitor(id, tags));
    }
}
