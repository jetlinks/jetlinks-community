package org.jetlinks.community.device.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.ProtocolSupportEntity;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.supports.protocol.management.ProtocolSupportManager;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class LocalProtocolSupportService extends GenericReactiveCrudService<ProtocolSupportEntity, String> {

    @Autowired
    private ProtocolSupportManager supportManager;

    @Autowired
    private DataReferenceManager referenceManager;

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
                   .flatMap(id -> supportManager.remove(id).thenReturn(id))
                   .as(super::deleteById);
    }

    public Mono<Boolean> deploy(String id) {
        return findById(Mono.just(id))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 1)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

    public Mono<Boolean> unDeploy(String id) {
        // 消息协议被使用时，不能禁用
        return referenceManager
            .assertNotReferenced(DataReferenceManager.TYPE_PROTOCOL, id)
            .then(findById(Mono.just(id)))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 0)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

}
