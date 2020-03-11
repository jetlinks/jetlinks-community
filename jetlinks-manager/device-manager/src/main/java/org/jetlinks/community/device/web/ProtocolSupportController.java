package org.jetlinks.community.device.web;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.web.protocol.ProtocolDetail;
import org.jetlinks.community.device.web.protocol.ProtocolInfo;
import org.jetlinks.community.device.web.protocol.TransportInfo;
import org.jetlinks.community.device.web.request.ProtocolDecodeRequest;
import org.jetlinks.community.device.web.request.ProtocolEncodeRequest;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.device.service.LocalProtocolSupportService;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/protocol")
@Authorize
@Resource(id = "protocol-supports", name = "协议管理")
public class ProtocolSupportController implements
    ReactiveServiceCrudController<ProtocolSupportEntity, String> {

    @Autowired
    @Getter
    private LocalProtocolSupportService service;

    @Autowired
    private ProtocolSupports protocolSupports;

    @Autowired
    private List<ProtocolSupportLoaderProvider> providers;

    @Autowired
    private ProtocolSupportLoader supportLoader;

    @PostMapping("/{id}/_deploy")
    @SaveAction
    public Mono<Boolean> deploy(@PathVariable String id) {
        return service.deploy(id);
    }

    @PostMapping("/{id}/_un-deploy")
    @SaveAction
    public Mono<Boolean> unDeploy(@PathVariable String id) {
        return service.unDeploy(id);
    }

    //获取支持的协议类型
    @GetMapping("/providers")
    @Authorize(merge = false)
    public Flux<String> getProviders() {
        return Flux.fromIterable(providers)
            .map(ProtocolSupportLoaderProvider::getProvider);
    }

    @GetMapping("/supports")
    @Authorize(merge = false)
    public Flux<ProtocolInfo> allProtocols() {
        return protocolSupports.getProtocols()
            .map(ProtocolInfo::of);
    }

    @GetMapping("/{id}/{transport}/configuration")
    @QueryAction
    @Authorize(merge = false)
    public Mono<ConfigMetadata> getTransportConfiguration(@PathVariable String id, @PathVariable DefaultTransport transport) {
        return protocolSupports.getProtocol(id)
            .flatMap(support -> support.getConfigMetadata(transport));
    }

    @GetMapping("/{id}/transports")
    @Authorize(merge = false)
    public Flux<TransportInfo> getAllTransport(@PathVariable String id) {
        return protocolSupports
            .getProtocol(id)
            .flatMapMany(ProtocolSupport::getSupportedTransport)
            .distinct()
            .map(TransportInfo::of);
    }

    @PostMapping("/convert")
    @QueryAction
    public Mono<ProtocolDetail> convertToDetail(@RequestBody Mono<ProtocolSupportEntity> entity) {
        return entity.map(ProtocolSupportEntity::toDeployDefinition)
            .doOnNext(def -> def.setId("_debug"))
            .flatMap(def -> supportLoader.load(def))
            .flatMap(ProtocolDetail::of);
    }

    @PostMapping("/decode")
    @SaveAction
    public Mono<String> decode(@RequestBody Mono<ProtocolDecodeRequest> entity) {
        return entity
            .<Object>flatMapMany(request -> {
                ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                supportEntity.setId("_debug");
                return supportLoader.load(supportEntity)
                    .flatMapMany(protocol -> request
                        .getRequest()
                        .doDecode(protocol, null));
            })
            .collectList()
            .map(JSON::toJSONString)
            .onErrorResume(err-> Mono.just(StringUtils.throwable2String(err)));
    }

    @PostMapping("/encode")
    @SaveAction
    public  Mono<String> encode(@RequestBody Mono<ProtocolEncodeRequest> entity) {
        return entity
            .flatMapMany(request -> {
                ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                supportEntity.setId("_debug");
                return supportLoader.load(supportEntity)
                    .flatMapMany(protocol -> request
                        .getRequest()
                        .doEncode(protocol, null));
            })
            .collectList()
            .map(JSON::toJSONString)
            .onErrorResume(err-> Mono.just(StringUtils.throwable2String(err)));
    }

    @GetMapping("/units")
    @Authorize(merge = false)
    public Flux<ValueUnit> allUnits() {
        return Flux.fromIterable(ValueUnits.getAllUnit());
    }

}
