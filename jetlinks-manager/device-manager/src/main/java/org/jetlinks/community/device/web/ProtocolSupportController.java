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
package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBufAllocator;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Generated;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.device.web.request.ProtocolDecodeRequest;
import org.jetlinks.community.device.web.request.ProtocolEncodeRequest;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.protocol.ProtocolDetail;
import org.jetlinks.community.protocol.ProtocolInfo;
import org.jetlinks.community.protocol.ProtocolSupportEntity;
import org.jetlinks.community.protocol.TransportDetail;
import org.jetlinks.community.protocol.service.LocalProtocolSupportService;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/protocol")
@Authorize
@Resource(id = "protocol-supports", name = "协议管理")
@Tag(name = "协议管理")
public class ProtocolSupportController
    implements ReactiveServiceCrudController<ProtocolSupportEntity, String> {

    @Autowired
    @Getter
    @Generated
    private LocalProtocolSupportService service;

    @Autowired
    private ProtocolSupports protocolSupports;

    @Autowired
    private List<ProtocolSupportLoaderProvider> providers;

    @Autowired
    private ProtocolSupportLoader supportLoader;

    @Autowired
    private FileManager fileManager;

    @PostMapping("/{id}/_deploy")
    @SaveAction
    @Operation(summary = "发布协议")
    @Deprecated
    public Mono<Boolean> deploy(@PathVariable String id) {
        return service.deploy(id);
    }

    @PostMapping("/{id}/_un-deploy")
    @SaveAction
    @Operation(summary = "取消发布")
    @Deprecated
    public Mono<Boolean> unDeploy(@PathVariable String id) {
        return service.unDeploy(id);
    }

    @GetMapping("/{id:.+}/exists")
    @QueryAction
    @Operation(summary = "验证协议ID是否存在")
    public Mono<Boolean> idValidate(@PathVariable @Parameter(description = "协议ID") String id) {
        return Flux.merge(service.findById(id),
                          protocolSupports.getProtocols()
                                          .filter(protocol -> protocol.getId().equals(id)))
                   .hasElements();
    }

    //获取支持的协议类型
    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取当前支持的协议类型")
    public Flux<String> getProviders() {
        return Flux
            .fromIterable(providers)
            .map(ProtocolSupportLoaderProvider::getProvider);
    }

    @GetMapping("/supports")
    @Authorize(merge = false)
    @Operation(summary = "获取当前支持的协议")
    public Flux<ProtocolInfo> allProtocols(@Parameter(hidden = true) QueryParamEntity query) {
        return protocolSupports
            .getProtocols()
            .collectMap(ProtocolSupport::getId)
            .flatMapMany(protocols -> service.createQuery()
                .setParam(query)
                .fetch()
                .index()
                .flatMap(tp2 -> Mono
                    .justOrEmpty(protocols.get(tp2.getT2().getId()))
                    .map(ignore -> ProtocolInfo.of(tp2.getT2()))
                    .map(protocolInfo -> Tuples.of(tp2.getT1(), protocolInfo))))
            .sort(Comparator.comparingLong(Tuple2::getT1))
            .map(Tuple2::getT2);

    }

    @GetMapping("/supports/{transport}")
    @Authorize(merge = false)
    @Operation(summary = "获取支持指定传输协议的消息协议")
    public Flux<ProtocolInfo> getSupportTransportProtocols(@PathVariable String transport,
                                                           @Parameter(hidden = true) QueryParamEntity query) {
        return service.getSupportTransportProtocols(transport,query);
    }

    @GetMapping("/{id}/{transport}/configuration")
    @QueryAction
    @Authorize(merge = false)
    @Operation(summary = "获取协议对应使用传输协议的配置元数据")
    public Mono<ConfigMetadata> getTransportConfiguration(@PathVariable @Parameter(description = "协议ID") String id,
                                                          @PathVariable @Parameter(description = "传输协议") String transport) {
        return service.getTransportConfiguration(id, transport);
    }

    @GetMapping("/{id}/{transport}/metadata")
    @QueryAction
    @Authorize(merge = false)
    @Operation(summary = "获取协议设置的默认物模型")
    public Mono<String> getDefaultMetadata(@PathVariable @Parameter(description = "协议ID") String id,
                                           @PathVariable @Parameter(description = "传输协议") String transport) {
        return service.getDefaultMetadata(id,transport);
    }

    @GetMapping("/{id}/transports")
    @Authorize(merge = false)
    @Operation(summary = "获取协议支持的传输协议")
    public Flux<TransportDetail> getAllTransport(@PathVariable @Parameter(description = "协议ID") String id) {
        return protocolSupports
            .getProtocol(id)
            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, id))
            .flatMapMany(protocol -> protocol
                .getSupportedTransport()
                .distinct()
                .flatMap(transport -> TransportDetail.of(protocol, transport)));
    }

    @GetMapping("/{id}/transport/{transport}")
    @Authorize(merge = false)
    @Operation(summary = "获取消息协议对应的传输协议信息")
    public Mono<TransportDetail> getTransportDetail(@PathVariable @Parameter(description = "协议ID") String id,
                                                    @PathVariable @Parameter(description = "传输协议") String transport) {
        return service.getTransportDetail(id, transport);
    }

    @PostMapping("/{id}/detail")
    @QueryAction
    @Operation(summary = "获取协议详情")
    public Mono<ProtocolDetail> protocolDetail(@PathVariable String id) {
        return protocolSupports
            .getProtocol(id)
            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, id))
            .flatMap(ProtocolDetail::of);
    }

    @PostMapping("/convert")
    @QueryAction
    @Hidden
    public Mono<ProtocolDetail> convertToDetail(@RequestParam(required = false) String transport, @RequestBody Mono<ProtocolSupportEntity> entity) {
        return entity.map(ProtocolSupportEntity::toDeployDefinition)
                     .doOnNext(def -> def.setId("_debug"))
                     .flatMap(def -> supportLoader.load(def))
                     .flatMap(support -> ProtocolDetail
                         .of(support, transport)
                         .doFinally(s -> support.dispose()));
    }

    @PostMapping("/decode")
    @SaveAction
    @Hidden
    public Mono<String> decode(@RequestBody Mono<ProtocolDecodeRequest> entity) {
        return entity
            .<Object>flatMapMany(request -> {
                ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                supportEntity.setId("_debug");
                return supportLoader
                    .load(supportEntity)
                    .flatMapMany(protocol -> Flux
                        .defer(() -> request
                            .getRequest()
                            .doDecode(protocol, null))
                        .doFinally(s -> protocol.dispose())
                    );
            })
            .collectList()
            .map(JSON::toJSONString)
            .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }

    @PostMapping("/encode")
    @SaveAction
    @Hidden
    public Mono<String> encode(@RequestBody Mono<ProtocolEncodeRequest> entity) {
        return entity
            .flatMapMany(request -> {
                ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                supportEntity.setId("_debug");
                return supportLoader
                    .load(supportEntity)
                    .flatMapMany(protocol -> Flux
                        .defer(() -> request
                            .getRequest()
                            .doEncode(protocol, null))
                        .doFinally(s -> protocol.dispose()));
            })
            .collectList()
            .map(JSON::toJSONString)
            .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }


    @GetMapping("/units")
    @Authorize(merge = false)
    @Operation(summary = "获取单位数据")
    public Flux<ValueUnit> allUnits() {
        return Flux
            .fromIterable(ValueUnits.getAllUnit())
            .distinct(ValueUnit::getId);
    }


    @PostMapping("/default-protocol/_save")
    @SaveAction
    @Operation(summary = "保存默认协议")
    public Mono<Void> saveDefaultProtocol() {

        String defaultProtocolName = "JetLinks官方协议";
        String fileNeme = "jetlinks-official-protocol-3.0-SNAPSHOT.jar";
        return fileManager
            .saveFile(fileNeme,
                      DataBufferUtils.read(new ClassPathResource(fileNeme),
                                           new NettyDataBufferFactory(ByteBufAllocator.DEFAULT),
                                           1024))
            .flatMap(fileInfo -> {
                Map<String, Object> conf = new HashMap<>();
                conf.put("fileId", fileInfo.getId());
                conf.put("provider", "org.jetlinks.protocol.official.JetLinksProtocolSupportProvider");
                conf.put("location", fileInfo.getAccessUrl());
                ProtocolSupportEntity entity = new ProtocolSupportEntity();
                entity.setId(DigestUtils.md5Hex(defaultProtocolName));
                entity.setType("jar");
                entity.setName(defaultProtocolName);
                entity.setState((byte) 1);
                entity.setDescription("JetLinks官方协议包");
                entity.setConfiguration(conf);
                return getService().save(entity);
            })
            .then();
    }

}
