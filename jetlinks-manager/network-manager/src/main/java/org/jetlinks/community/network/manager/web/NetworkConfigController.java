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
package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.manager.entity.NetworkConfigEntity;
import org.jetlinks.community.network.manager.info.NetworkConfigAliveInfo;
import org.jetlinks.community.network.manager.service.NetworkChannelHandler;
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
@Tag(name = "网络组件管理")
@AllArgsConstructor
public class NetworkConfigController implements ReactiveServiceCrudController<NetworkConfigEntity, String> {

    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    private final NetworkChannelHandler networkChannelHandler;

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
       return networkChannelHandler.getNetworkInfo(QueryParamEntity.of().noPaging(), networkType);
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
        return  networkChannelHandler.getAliveNetworkInfo(networkType, include, query);
    }

    @PostMapping("/_alive")
    @QueryAction
    @Operation(summary = "获取多个类型下可用的网络组件信息")
    public Flux<ChannelInfo> getAliveNetworkInfoForMoreType(@RequestParam(required = false) @Parameter(description = "包含指定的网络组件ID") String include,
                                                            @RequestBody @Parameter(hidden = true) Mono<NetworkConfigAliveInfo> aliveInfoMono) {
        return aliveInfoMono
            .flatMapMany(info -> {
                QueryParamEntity query = info.getQuery();
                return  networkChannelHandler
                    .getAliveNetworkInfoForMoreType(info.getNetworkTypes(), include, query);
            });

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
