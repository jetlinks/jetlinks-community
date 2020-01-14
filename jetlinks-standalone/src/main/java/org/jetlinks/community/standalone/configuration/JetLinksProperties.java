package org.jetlinks.community.standalone.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Map;

@ConfigurationProperties(prefix = "jetlinks")
@Getter
@Setter
public class JetLinksProperties {

    private String serverId;

    private String clusterName ="default";

    private Map<String, Long> transportLimit;

    @PostConstruct
    @SneakyThrows
    public void init() {
        if (serverId == null) {
            serverId = InetAddress.getLocalHost().getHostName();
        }
    }
}
