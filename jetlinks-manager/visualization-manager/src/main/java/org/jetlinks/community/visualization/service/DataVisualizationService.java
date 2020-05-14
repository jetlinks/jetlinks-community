package org.jetlinks.community.visualization.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.visualization.entity.DataVisualizationEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DataVisualizationService extends GenericReactiveCrudService<DataVisualizationEntity,String> {

    @Override
    public Mono<SaveResult> save(Publisher<DataVisualizationEntity> entityPublisher) {
        return Flux.from(entityPublisher)
            .doOnNext(DataVisualizationEntity::applyId)
            .as(super::save);
    }

}
