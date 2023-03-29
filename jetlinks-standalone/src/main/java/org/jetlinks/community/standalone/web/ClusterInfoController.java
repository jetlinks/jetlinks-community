package org.jetlinks.community.standalone.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.cluster.ServerNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/cluster")
@RestController
@Authorize
@Tag(name = "系统管理")
public class ClusterInfoController {

    @Autowired
    private ClusterManager clusterManager;

    @GetMapping("/nodes")
    @Operation(summary = "获取集群节点")
    public Flux<ServerNode> getServerNodes() {
        return Flux.fromIterable(clusterManager.getHaManager().getAllNode());
    }

}
