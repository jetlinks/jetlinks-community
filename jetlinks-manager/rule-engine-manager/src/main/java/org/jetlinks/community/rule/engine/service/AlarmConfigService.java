package org.jetlinks.community.rule.engine.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.AlarmConfigEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AlarmConfigService extends GenericReactiveCrudService<AlarmConfigEntity, String> {


    private final AlarmRecordService alarmRecordService;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;


    /**
     * 处理告警
     *
     * @param id         告警记录ID
     * @param handleInfo 处理信息
     */
    public Mono<Void> handleAlarm(String id, AlarmHandleInfo handleInfo) {
        return alarmRecordService
            .findById(id)
            .flatMap(alarmRecord -> alarmRecordService
                .changeRecordState(handleInfo.getState(), handleInfo.getId())
                .then(handleHistoryRepository
                          .save(AlarmHandleHistoryEntity.of(handleInfo.getId(),
                                                            alarmRecord.getAlarmConfigId(),
                                                            alarmRecord.getAlarmTime(),
                                                            handleInfo)))
                .then());
    }


    //同步场景修改后的数据到告警配置中
    @EventListener
    public void handleSceneSaved(EntitySavedEvent<SceneEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(this::updateByScene)
            .then()));
    }

    //同步场景修改后的数据到告警配置中
    @EventListener
    public void handleSceneSaved(EntityModifyEvent<SceneEntity> event) {

        Map<String, SceneEntity> before = event
            .getBefore()
            .stream()
            .collect(Collectors.toMap(SceneEntity::getId, Function.identity()));

        event.async(
            Flux.fromIterable(event.getAfter())
                .filter(scene -> StringUtils.hasText(scene.getName())
                            && before.get(scene.getId()) != null && (
                            !Objects.equals(before.get(scene.getId()).getName(), scene.getName())
                                ||
                                !Objects.equals(before.get(scene.getId()).getTriggerType(), scene.getTriggerType())
                        )
                )
                .flatMap(this::updateByScene)
        );
    }


    public Mono<Void> updateByScene(SceneEntity entity) {
        return createUpdate()
            .set(AlarmConfigEntity::getSceneName, entity.getName())
            .set(AlarmConfigEntity::getSceneTriggerType, entity.getTriggerType())
            .where(AlarmConfigEntity::getSceneId, entity.getId())
            .execute()
            .then();
    }

    public Mono<Void> enable(String id) {
        return createUpdate()
            .set(AlarmConfigEntity::getState, AlarmState.enabled)
            .where(AlarmConfigEntity::getId, id)
            .execute()
            .then();
    }

    public Mono<Void> disable(String id) {
        return createUpdate()
            .set(AlarmConfigEntity::getState, AlarmState.disabled)
            .where(AlarmConfigEntity::getId, id)
            .execute()
            .then();
    }
}
