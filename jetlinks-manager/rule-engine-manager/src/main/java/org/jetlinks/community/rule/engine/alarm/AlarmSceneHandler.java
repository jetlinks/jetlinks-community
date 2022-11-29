package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.entity.AlarmConfigEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.scene.SceneData;
import org.jetlinks.community.rule.engine.scene.SceneFilter;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.topic.Topics;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bestfeng
 */
@Component
@AllArgsConstructor
@Deprecated
public class AlarmSceneHandler implements SceneFilter, CommandLineRunner {

    private final EventBus eventBus;

    private final AlarmConfigService alarmConfigService;

    private final AlarmRecordService alarmRecordService;

    private final AlarmHistoryService alarmHistoryService;

    private final ApplicationEventPublisher publisher;
    /**
     * {
     * "场景Id":{
     * "告警配置ID":"告警配置信息"
     * }
     * }
     */
    private final Map<String, Map<String, AlarmConfigEntity>> alarmConfigCache = new ConcurrentHashMap<>(16);


    @Override
    public Mono<Boolean> filter(SceneData data) {
        Map<String, AlarmConfigEntity> alarmConfigMap = alarmConfigCache.get(data.getRule().getId());
        if (alarmConfigMap != null) {
            return Flux
                .fromIterable(alarmConfigMap.values())
                .flatMap(alarmConfig -> AlarmTarget
                    .of(alarmConfig.getTargetType())
                    .convert(AlarmData.of(alarmConfig.getId(), alarmConfig.getName(), data.getRule().getId(), data.getRule().getName(), data.getOutput()))
                    .flatMap(targetInfo -> {
                        AlarmRecordEntity record = ofRecord(targetInfo, alarmConfig);
                        //修改告警记录
                        return alarmRecordService
                            .save(record)
                            //推送告警信息到消息网关中  topic格式: /alarm/{targetType}/{targetId}/{alarmConfigId}/record
                            //fixme 已经告警中则不再推送事件
                            .then(publishAlarmRecord(record, alarmConfig))
                            //保存告警日志
                            .then(Mono.defer(() -> {
                                AlarmHistoryInfo alarmHistoryInfo = AlarmHistoryInfo
                                    .of(record.getId(),
                                        targetInfo,
                                        data,
                                        alarmConfig);
                                //推送告警历史数据
                                publisher.publishEvent(alarmHistoryInfo);
                                return alarmHistoryService
                                    .save(alarmHistoryInfo);
                            }));
                    }))
                .then()
                .thenReturn(true);
        }
        return Mono.empty();
    }

    public Mono<Void> publishAlarmRecord(AlarmRecordEntity record, AlarmConfigEntity config) {
        String topic = Topics.alarm(record.getTargetType(), record.getTargetId(), record.getAlarmConfigId());

        return eventBus
            .publish(topic, record)
            .then();
    }


    @EventListener
    public void handleAlarmConfigCreated(EntityCreatedEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(this::handleAlarmConfigCRU)
            .then()));
    }

    @EventListener
    public void handleAlarmConfigModify(EntityModifyEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getAfter())
            .flatMap(this::handleAlarmConfigCRU)
            .then()));
    }

    @EventListener
    public void handleAlarmConfigSaved(EntitySavedEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(this::handleAlarmConfigCRU)
            .then()));
    }

    @EventListener
    public void beforeAlarmConfigDelete(EntityBeforeDeleteEvent<AlarmConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> alarmRecordService
                    .createQuery()
                    .where(AlarmRecordEntity::getAlarmConfigId, e.getId())
                    .and(AlarmRecordEntity::getState, AlarmRecordState.warning)
                    .count()
                    .doOnNext(i -> {
                        if (i > 0) {
                            throw new BusinessException("error.cannot_delete_alarm_config_with_warnning_record");
                        }
                    })
                )
        );
    }


    @EventListener
    public void handleAlarmConfigDelete(EntityDeletedEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(this::handleAlarmConfigDelete)
            .then()));
    }

    //处理告警配置创建、修改、保存
    private Mono<Void> handleAlarmConfigCRU(AlarmConfigEntity config) {
        if (AlarmState.enabled.equals(config.getState())) {
            return doAlarmConfigCRU(config);
        } else {
            return handleAlarmConfigDelete(config);
        }
    }

    //处理告警配置删除
    private Mono<Void> handleAlarmConfigDelete(AlarmConfigEntity alarmConfig) {
        return doAlarmConfigDelete(alarmConfig);
    }


    //告警配置创建、修改、保存
    private Mono<Void> doAlarmConfigCRU(AlarmConfigEntity alarmConfig) {
        return Mono
            .fromSupplier(() -> alarmConfigCache.compute(alarmConfig.getSceneId(), (k, v) -> {
                if (v == null) {
                    v = new ConcurrentHashMap<>();
                }
                v.put(alarmConfig.getId(), alarmConfig);
                return v;
            }))
            .then();
    }

    //告警配置删除
    private Mono<Void> doAlarmConfigDelete(AlarmConfigEntity alarmConfig) {
        return Mono
            .fromSupplier(() -> alarmConfigCache.computeIfPresent(alarmConfig.getSceneId(), (k, v) -> {
                v.remove(alarmConfig.getId());
                if (v.size() == 0) {
                    alarmConfigCache.remove(alarmConfig.getSceneId());
                }
                return v;
            }))
            .then();
    }

    private AlarmRecordEntity ofRecord(AlarmTargetInfo targetInfo,
                                       AlarmConfigEntity alarmConfigEntity) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setAlarmConfigId(alarmConfigEntity.getId());
        entity.setState(AlarmRecordState.warning);
        entity.setAlarmTime(System.currentTimeMillis());
        entity.setLevel(alarmConfigEntity.getLevel());
        entity.setTargetType(targetInfo.getTargetType());
        entity.setTargetName(targetInfo.getTargetName());
        entity.setTargetId(targetInfo.getTargetId());
        entity.setAlarmName(alarmConfigEntity.getName());
        entity.generateId();
        return entity;
    }


    @Override
    public void run(String... args) throws Exception {
        alarmConfigService
            .createQuery()
            .where(AlarmConfigEntity::getState, AlarmState.enabled)
            .fetch()
            .flatMap(this::doAlarmConfigCRU)
            .subscribe();
    }
}
