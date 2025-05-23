/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.configure.cluster;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.trace.TraceHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
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
        Cluster.setup(id, id, Collections.emptyMap());
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
