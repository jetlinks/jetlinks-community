package org.jetlinks.community.network.manager.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.NetworkProvider;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.web.response.NetworkTypeInfo;
import org.springframework.util.StringUtils;
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

    @GetMapping("/{networkType}/_detail")
    @QueryAction
    public Flux<NetworkConfigInfo> getNetworkInfo(@PathVariable String networkType) {
        return configService.createQuery()
            .where(NetworkConfigEntity::getType, networkType)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .map(config -> NetworkConfigInfo.of(config.getId(), config.getName(), ""));
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class NetworkConfigInfo {
        private String id;

        private String name;

        private String address;

        public String getDetail() {
            if (StringUtils.hasText(address)) {
                return name + "(" + address + ")";
            }
            return name;
        }
    }

    @GetMapping("/supports")
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager
            .getProviders())
            .map(NetworkProvider::getType)
            .map(NetworkTypeInfo::of);
    }

    @PostMapping("/{id}/_start")
    @SaveAction
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
    @SaveAction
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
