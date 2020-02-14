package org.jetlinks.community.device.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.device.service.LocalProtocolSupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .map(TransportInfo::of);
    }

    @GetMapping("/units")
    @Authorize(merge = false)
    public Flux<ValueUnit> allUnits() {
        return Flux.fromIterable(ValueUnits.getAllUnit());
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class TransportInfo {
        private String id;

        private String name;

        static TransportInfo of(Transport support) {
            return of(support.getId(), support.getName());
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    @NoArgsConstructor
    public static class ProtocolInfo {
        private String id;

        private String name;

        static ProtocolInfo of(ProtocolSupport support) {
            return of(support.getId(), support.getName());
        }
    }
}
