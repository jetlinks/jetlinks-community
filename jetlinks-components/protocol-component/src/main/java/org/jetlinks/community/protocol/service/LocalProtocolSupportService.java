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
package org.jetlinks.community.protocol.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.protocol.ProtocolInfo;
import org.jetlinks.community.protocol.ProtocolSupportEntity;
import org.jetlinks.community.protocol.TransportDetail;
import org.jetlinks.community.reference.DataReferenceManager;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class LocalProtocolSupportService extends GenericReactiveCrudService<ProtocolSupportEntity, String> {

    private final DataReferenceManager referenceManager;

    private final ProtocolSupports protocolSupports;

    @Transactional
    public Mono<Boolean> deploy(String id) {
        return findById(id)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 1)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

    @Transactional
    public Mono<Boolean> unDeploy(String id) {
        // 消息协议被使用时，不能禁用
        return referenceManager
            .assertNotReferenced(DataReferenceManager.TYPE_PROTOCOL, id, "error.protocol_referenced")
            .then(findById(id))
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .flatMap(r -> createUpdate()
                .set(ProtocolSupportEntity::getState, 0)
                .where(ProtocolSupportEntity::getId, id)
                .execute())
            .map(i -> i > 0);
    }

    @Transactional
    public Mono<Void> addProtocolsThenDeploy(List<ProtocolSupportEntity> entities) {
        return Flux.fromIterable(entities)
                   .doOnNext(entity -> Assert.hasText(entity.getId(), "message.Id_cannot_be_empty"))
                   .as(this::save)
                   .thenMany(Flux.fromIterable(entities))
                   .flatMap(entity -> deploy(entity.getId())
                       .onErrorResume((err) -> {
                           log.warn("同步协议失败:", err);
                           return Mono.empty();
                       }))
                   .then();

    }

    public Flux<ProtocolInfo> getSupportTransportProtocols(String transport,
                                                           QueryParamEntity query) {
        return protocolSupports
            .getProtocols()
            .collectMap(ProtocolSupport::getId)
            .flatMapMany(protocols -> this.createQuery()
                                          .setParam(query)
                                          .fetch()
                                          .index()
                                          .flatMap(tp2 -> Mono
                                              .justOrEmpty(protocols.get(tp2.getT2().getId()))
                                              .filterWhen(support -> support
                                                  .getSupportedTransport()
                                                  .filter(t -> t.isSame(transport))
                                                  .hasElements())
                                              .map(ignore -> ProtocolInfo.of(tp2.getT2()))
                                              .map(protocolInfo -> Tuples.of(tp2.getT1(), protocolInfo))))
            .sort(Comparator.comparingLong(Tuple2::getT1))
            .map(Tuple2::getT2);
    }

    public Mono<TransportDetail> getTransportDetail(String id, String transport) {
        return protocolSupports
            .getProtocol(id)
            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, id))
            .flatMapMany(protocol -> protocol
                .getSupportedTransport()
                .filter(trans -> trans.isSame(transport))
                .distinct()
                .flatMap(_transport -> TransportDetail.of(protocol, _transport)))
            .singleOrEmpty();
    }

    public Mono<ConfigMetadata> getTransportConfiguration(String id, String transport) {
        return protocolSupports
            .getProtocol(id)
            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, id))
            .flatMap(support -> support.getConfigMetadata(Transport.of(transport)));
    }

    public Mono<String> getDefaultMetadata(String id, String transport) {
        return protocolSupports
            .getProtocol(id)
            .onErrorMap(e -> new BusinessException("error.unable_to_load_protocol_by_access_id", 404, id))
            .flatMap(support -> support
                .getDefaultMetadata(Transport.of(transport))
                .flatMap(support.getMetadataCodec()::encode)
            ).defaultIfEmpty("{}");
    }

}
