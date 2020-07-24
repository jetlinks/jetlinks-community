package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeviceAlarmService extends GenericReactiveCrudService<DeviceAlarmEntity, String> {

    private final RuleInstanceService instanceService;

    @SuppressWarnings("all")
    public DeviceAlarmService(RuleInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceAlarmEntity> entityPublisher) {
        return Flux.from(entityPublisher)
            .doOnNext(e -> e.setState(null))
            .flatMap(alarm -> {
                if (StringUtils.hasText(alarm.getId())) {
                    return instanceService
                        .save(Mono.just(alarm.toRuleInstance()))
                        .thenReturn(alarm);
                }
                return Mono.just(alarm);
            })
            .as(DeviceAlarmService.super::save);
    }

    public Mono<Void> start(String id) {
        return findById(id)
            .flatMap(this::doStart);
    }

    public Mono<Void> stop(String id) {
        return instanceService.stop(id)
            .then(createUpdate()
                .set(DeviceAlarmEntity::getState,AlarmState.stopped)
                .where(DeviceAlarmEntity::getId,id)
                .execute())
            .then();
    }

    private Mono<Void> doStart(DeviceAlarmEntity entity) {
        return instanceService
            .save(Mono.just(entity.toRuleInstance()))
            .then(instanceService.start(entity.getId()))
            .then(createUpdate()
                .set(DeviceAlarmEntity::getState, AlarmState.running)
                .where(entity::getId).execute())
            .then();
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .flatMap(id -> instanceService.stop(id)
                .then(instanceService.deleteById(Mono.just(id)))
                .then(DeviceAlarmService.super.deleteById(Mono.just(id)))
            ).reduce(Math::addExact);
    }

}
