package org.jetlinks.community.standalone.configuration;

import org.jetlinks.supports.protocol.management.ManagementProtocolSupports;
import org.springframework.boot.CommandLineRunner;

public class LazyInitManagementProtocolSupports extends ManagementProtocolSupports implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        init();
    }
}
