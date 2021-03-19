package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.network.DefaultNetworkType;
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
@Tag(name = "网络组件管理")
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
    @Operation(summary = "获取指定类型下所有网络组件信息")
    public Flux<NetworkConfigInfo> getNetworkInfo(@PathVariable
                                                  @Parameter(description = "网络组件类型") String networkType) {


        return configService
            .createQuery()
            .where(NetworkConfigEntity::getType, networkType)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .flatMap(config -> {
                Mono<NetworkConfigInfo> def = Mono.just(NetworkConfigInfo.of(config.getId(), config.getName(), ""));
                if (config.getState() == NetworkConfigState.enabled) {
                    return networkManager.getNetwork(DefaultNetworkType.valueOf(networkType.toUpperCase()), config.getId())
                        .map(server -> NetworkConfigInfo.of(config.getId(), config.getName(),""))
                        .onErrorResume(err -> def)
                        .switchIfEmpty(def);
                }
                return def;
            });
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class NetworkConfigInfo {
        @Schema(description = "网络组件ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "地址")
        private String address;

        @Schema(description = "详情")
        public String getDetail() {
            if (StringUtils.hasText(address)) {
                return name + "(" + address + ")";
            }
            return name;
        }
    }

    @GetMapping("/supports")
    @Operation(summary = "获取支持的网络组件类型")
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager
            .getProviders())
            .map(NetworkProvider::getType)
            .map(NetworkTypeInfo::of);
    }

    @PostMapping("/{id}/_start")
    @SaveAction
    @Operation(summary = "启动网络组件")
    public Mono<Void> start(@PathVariable
                            @Parameter(description = "网络组件ID") String id) {
        return configService.findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("配置[" + id + "]不存在")))
            .flatMap(conf -> configService.createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.reload(conf.lookupNetworkType(), id));
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止网络组件")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网络组件ID") String id) {
        return configService.findById(id)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("配置[" + id + "]不存在")))
            .flatMap(conf -> configService.createUpdate()
                .set(NetworkConfigEntity::getState, NetworkConfigState.disabled)
                .where(conf::getId)
                .execute()
                .thenReturn(conf))
            .flatMap(conf -> networkManager.shutdown(conf.lookupNetworkType(), id));
    }

}
