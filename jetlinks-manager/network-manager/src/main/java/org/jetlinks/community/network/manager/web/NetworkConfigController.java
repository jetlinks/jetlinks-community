package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.enums.NetworkConfigState;
import org.jetlinks.community.network.manager.service.NetworkChannelProvider;
import org.jetlinks.community.network.manager.service.NetworkConfigService;
import org.jetlinks.community.network.manager.web.response.NetworkTypeInfo;
import org.jetlinks.community.reference.DataReferenceManager;
import org.springframework.web.bind.annotation.*;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author zhouhao
 * @since 1.0
 **/
@RestController
@RequestMapping("/network/config")
@Resource(id = "network-config", name = "网络组件配置")
@Authorize
@Tag(name = "网络组件管理")
@AllArgsConstructor
public class NetworkConfigController implements ReactiveServiceCrudController<NetworkConfigEntity, String> {

    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    private final DataReferenceManager referenceManager;

    private final NetworkChannelProvider channelProvider;

    @Generated
    @Override
    public NetworkConfigService getService() {
        return configService;
    }


    @GetMapping("/{networkType}/_detail")
    @QueryAction
    @Operation(summary = "获取指定类型下全部的网络组件信息")
    public Flux<ChannelInfo> getNetworkInfo(@PathVariable
                                            @Parameter(description = "网络组件类型") String networkType) {
        NetworkProvider<?> provider = networkManager
            .getProvider(networkType)
            .orElseThrow(() -> new I18nSupportException("error.unsupported_network_type", networkType));

        return  configService
            .createQuery()
            .where(NetworkConfigEntity::getType, networkType)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .flatMap(entity -> toConfigInfo(entity, provider));
    }

    /**
     * 获取指定类型下可用的网络组件信息
     * <pre>{@code
     * POST /network/config/{networkType}/_alive?include=
     * }</pre>
     */
    @GetMapping("/{networkType}/_alive")
    @QueryAction
    @QueryOperation(summary = "获取指定类型下可用的网络组件信息")
    public Flux<ChannelInfo> getAliveNetworkInfo(@PathVariable
                                                 @Parameter(description = "网络组件类型") String networkType,
                                                 @Parameter(description = "包含指定的网络组件ID")
                                                 @RequestParam(required = false) String include,
                                                 @Parameter(hidden = true) QueryParamEntity query) {
        NetworkProvider<?> provider = networkManager
            .getProvider(networkType)
            .orElseThrow(() -> new I18nSupportException("error.unsupported_network_type", networkType));

        return configService
            .createQuery()
            .setParam(query)
            .where(NetworkConfigEntity::getType, networkType)
            .and(NetworkConfigEntity::getState, NetworkConfigState.enabled)
            .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
            .fetch()
            .filterWhen(config -> {
                if (provider.isReusable() || Objects.equals(config.getId(), include)) {
                    return Mono.just(true);
                }
                //判断是否已经被使用
                return referenceManager
                    .isReferenced(DataReferenceManager.TYPE_NETWORK, config.getId())
                    .as(BooleanUtils::not);
            })
            .flatMap(entity -> toConfigInfo(entity, provider));
    }

    private Mono<ChannelInfo> toConfigInfo(NetworkConfigEntity entity, NetworkProvider<?> provider) {
        return channelProvider.toChannelInfo(entity);
    }

    @GetMapping("/supports")
    @Operation(summary = "获取支持的网络组件类型")
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager.getProviders())
                   .map(NetworkProvider::getType)
                   .map(NetworkTypeInfo::of);
    }

    @PostMapping("/{id}/_start")
    @SaveAction
    @Operation(summary = "启动网络组件")
    public Mono<Void> start(@PathVariable
                            @Parameter(description = "网络组件ID") String id) {
        return configService.start(id);
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止网络组件")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网络组件ID") String id) {
        return configService.shutdown(id);
    }

}
