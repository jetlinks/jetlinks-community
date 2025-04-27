package org.jetlinks.community.rule.engine.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rule.engine")
public class RuleEngineProperties {

    private String clusterName = "jetlinks";

    private String serverId = "default";

    private String serverName = "default";

    /**
     * 规则命令空间,相同命令空间的集群节点才会被调度
     */
    private String namespace;


}
