package org.jetlinks.community.configure.device;

import io.micrometer.core.instrument.Gauge;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.core.device.session.DeviceSessionManager;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

@AllArgsConstructor
public class DeviceSessionMonitor {

    private MeterRegistryManager registryManager;

    private DeviceSessionManager sessionManager;

    private String name;

    public void init() {
        Gauge.builder(name, this::getTotalSession)
             .tag("server", sessionManager.getCurrentServerId())
             .register(registryManager.getMeterRegister("device_metrics"));
    }

    @SneakyThrows
    @SuppressWarnings("all")
    private long getTotalSession() {
        Mono<Long> session = sessionManager.totalSessions(true);
        Long val = null;
        if (session instanceof Callable) {
            val = ((Callable<Long>) session).call();
        } else {
            val = session
                .toFuture()
                .getNow(0L);
        }
        return val == null ? 0 : val;
    }
}
