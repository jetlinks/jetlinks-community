package org.jetlinks.community.network.manager.web;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.NetworkProvider;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.web.response.NetworkTypeInfo;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0
 **/
@RestController
@RequestMapping("/network/config")
@Resource(id = "network-config", name = "网络组件配置")
@Authorize
public class NetworkConfigController implements ReactiveServiceCrudController<NetworkConfigEntity, String> {

    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    public NetworkConfigController(NetworkConfigService configService, NetworkManager networkManager) {
        this.configService = configService;
        this.networkManager = networkManager;
    }

    @Override
    public NetworkConfigService getService() {
        return configService;
    }

    @GetMapping("/supports")
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager
            .getProviders())
            .map(NetworkProvider::getType)
            .map(NetworkTypeInfo::of);
    }

    @PostMapping("/{id}/_start")
    public Mono<Void> start(@PathVariable String id) {
        return configService.findById(id)
                .switchIfEmpty(Mono.error(()->new NotFoundException("配置[" + id + "]不存在")))
                .flatMap(conf -> configService.createUpdate()
                        .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                        .where(conf::getId)
                        .execute()
                        .thenReturn(conf))
                .flatMap(conf -> networkManager.reload(conf.getType(), id));
    }

    @PostMapping("/{id}/_shutdown")
    public Mono<Void> shutdown(@PathVariable String id) {
        return configService.findById(id)
                .switchIfEmpty(Mono.error(()->new NotFoundException("配置[" + id + "]不存在")))
                .flatMap(conf -> configService.createUpdate()
                        .set(NetworkConfigEntity::getState, NetworkConfigState.disabled)
                        .where(conf::getId)
                        .execute()
                        .thenReturn(conf))
                .flatMap(conf -> networkManager.shutdown(conf.getType(), id));
    }

}
