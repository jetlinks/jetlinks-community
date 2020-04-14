package org.jetlinks.community.rule.engine.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeviceAlarmService extends GenericReactiveCrudService<DeviceAlarmEntity, String> {

    private final RuleInstanceService instanceService;

    @SuppressWarnings("all")
    public DeviceAlarmService(RuleInstanceService instanceService) {
        this.instanceService = instanceService;
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


}
