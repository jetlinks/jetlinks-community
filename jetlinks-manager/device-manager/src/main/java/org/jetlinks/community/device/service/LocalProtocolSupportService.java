package org.jetlinks.community.device.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class LocalProtocolSupportService extends GenericReactiveCrudService<ProtocolSupportEntity, String> {

    @Autowired
    private ProtocolSupportManager supportManager;

    @Autowired
    private ProtocolSupportLoader loader;

    public Mono<Boolean> deploy(String id) {
        return findById(Mono.just(id))
                .switchIfEmpty(Mono.error(NotFoundException::new))
                .map(ProtocolSupportEntity::toDeployDefinition)
                .flatMap(def->loader.load(def).thenReturn(def))
                .onErrorMap(err->new BusinessException("无法加载协议:"+err.getMessage(),err))
                .flatMap(supportManager::save)
                .flatMap(r -> createUpdate()
                        .set(ProtocolSupportEntity::getState, 1)
                        .where(ProtocolSupportEntity::getId, id)
                        .execute())
                .map(i -> i > 0);
    }

    public Mono<Boolean> unDeploy(String id) {
        return findById(Mono.just(id))
                .switchIfEmpty(Mono.error(NotFoundException::new))
                .map(ProtocolSupportEntity::toUnDeployDefinition)
                .flatMap(supportManager::save)
                .flatMap(r -> createUpdate()
                        .set(ProtocolSupportEntity::getState, 0)
                        .where(ProtocolSupportEntity::getId, id)
                        .execute())
                .map(i -> i > 0);
    }

}
