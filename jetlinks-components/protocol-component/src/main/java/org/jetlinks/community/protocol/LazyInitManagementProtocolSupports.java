package org.jetlinks.community.protocol;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.supports.protocol.management.DefaultProtocolSupportManager;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Slf4j
@Getter
@Setter
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LazyInitManagementProtocolSupports extends DefaultProtocolSupportManager implements CommandLineRunner {

    public LazyInitManagementProtocolSupports(EventBus eventBus,
                                              ClusterManager clusterManager,
                                              ProtocolSupportLoader loader) {
        super(eventBus, clusterManager.getCache("__protocol_supports"), loader);
    }


    public void init() {
        super.init();
    }

    @Override
    public void run(String... args) {
        init();
    }


}
