package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.network.resource.NetworkResource;
import org.jetlinks.community.network.resource.NetworkResourceManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/network/resources")
@AllArgsConstructor
@Authorize
@Tag(name = "网络资源管理")
public class NetworkResourceController {

    private final NetworkResourceManager resourceManager;

    @GetMapping("/alive")
    @Operation(summary = "获取可用的资源信息")
    public Flux<NetworkResource> getResources() {
        return resourceManager
            .getAliveResources()
            //只获取绑定全部网卡的信息
            .filter(NetworkResource::hostIsBindAll)
            .groupBy(NetworkResource::getHost)
            .flatMap(group -> {
                String host = group.key();
                return group
                    //只获取端口交集
                    .reduce(NetworkResource.of(host), (a, b) -> a.retainPorts(b.getPorts()));
            });
    }

    @GetMapping("/alive/_all")
    @Operation(summary = "获取全部集群节点的资源信息")
    public Flux<NetworkResource> getAllResources() {
        return resourceManager.getAliveResources();
    }


    @GetMapping("/alive/_current")
    @Operation(summary = "获取当前集群节点的资源信息")
    public Flux<NetworkResource> getCurrentAliveResources() {
        return resourceManager.getAliveResources();
    }

}
