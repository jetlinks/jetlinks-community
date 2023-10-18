package org.jetlinks.community.configure.cluster;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jetlinks.cluster")
@Getter
@Setter
public class ClusterProperties {

    private static String NAME = "default";

    private String id = "default";

    private String name = NAME;

    //集群节点对外暴露的host
    private String externalHost = "127.0.0.1";

    //集群节点对外暴露的端口
    private Integer externalPort;

    //集群本地监听端口
    private int port;

    //集群rpc对外暴露的host
    private String rpcExternalHost = "127.0.0.1";

    //集群rpc对外暴露的端口
    private Integer rpcExternalPort;

    //集群rpc调用端口
    private int rpcPort;

    private List<String> seeds = new ArrayList<>();

    public void setId(String id) {
        this.id = id;
        Cluster.ID = id;
        TraceHolder.setupGlobalName(id);
    }

    public void setName(String name) {
        this.name = name;
        NAME = name;
    }

    public static String globalName() {
        return NAME;
    }


}
