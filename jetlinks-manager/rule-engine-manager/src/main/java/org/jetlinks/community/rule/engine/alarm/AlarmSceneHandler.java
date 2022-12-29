package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                    .convert(AlarmData.of(alarmConfig.getId(), alarmConfig.getName(), data.getRule().getId(), data
                        .getRule()
                        .getName(), data.getOutput()))
                    .flatMap(targetInfo -> {
                        AlarmRecordEntity record = ofRecord(targetInfo, alarmConfig);

                        return alarmRecordService
                            .createUpdate()
                            .set(record)
                            .where(AlarmRecordEntity::getId, record.getId())
                            // 如果已存在告警中的记录，则更新
                            .and(AlarmRecordEntity::getState, AlarmRecordState.warning)
                            .execute()
                            .flatMap(warningRecordCount -> {
                                if (warningRecordCount == 0) {
                                    return alarmRecordService
                                        .save(record)
                                        .thenReturn(warningRecordCount);
                                } else {
                                    return Mono.just(warningRecordCount);
                                }
                            })
                            .flatMap(warningRecordCount -> {
                                //保存告警日志
                                AlarmHistoryInfo alarmHistoryInfo = AlarmHistoryInfo
                                    .of(record.getId(),
                                        targetInfo,
                                        data,
                                        alarmConfig);
                                //推送告警历史数据
                                publisher.publishEvent(alarmHistoryInfo);
                                return alarmHistoryService
                                    .save(alarmHistoryInfo)
                                    //已经告警中则不再推送事件
                                    .then(Mono.defer(() -> warningRecordCount == 0 ?
                                        //推送告警信息到消息网关中  topic格式: /alarm/{targetType}/{targetId}/{alarmConfigId}/record
                                        publishAlarmRecord(alarmHistoryInfo, alarmConfig) :
                                        Mono.empty()));
                            });
                    }))
                .then()
                .thenReturn(true);
        }
        return Mono.empty();
    }

    public Mono<Void> publishAlarmRecord(AlarmHistoryInfo record, AlarmConfigEntity config) {
        String topic = Topics.alarm(record.getTargetType(), record.getTargetId(), record.getAlarmConfigId());
        return eventBus
            .publish(topic, record)
            .then();
    }

    @Subscribe(value = "/_sys/alarm/config/deleted", features = Subscription.Feature.broker)
    public Mono<Void> HandleAlarmConfigDelete(AlarmConfigEntity alarmConfig) {
        return doAlarmConfigDelete(alarmConfig);

    }


    @Subscribe(value = "/_sys/alarm/config/created,saved,modified", features = Subscription.Feature.broker)
    public Mono<Void> handleAlarmConfigCRU(AlarmConfigEntity alarmConfig) {
        return doAlarmConfigCRU(alarmConfig);
    }


    @EventListener
    public void handleAlarmConfigCreated(EntityCreatedEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(config -> handleAlarmConfigCRU("/_sys/alarm/config/created", config))
            .then()));
    }

    @EventListener
    public void handleAlarmConfigModify(EntityModifyEvent<AlarmConfigEntity> event) {
        Map<String, AlarmConfigEntity> beforeMap = event
            .getBefore()
            .stream()
            .collect(Collectors.toMap(AlarmConfigEntity::getId, Function.identity()));

        event.async(Flux
                        .fromIterable(event.getAfter())
                        .flatMap(config -> handleAlarmConfigCRU("/_sys/alarm/config/modified", config).thenReturn(config))
                        .filter(config -> {
                            AlarmConfigEntity before = beforeMap.get(config.getId());
                            if (before != null) {
                                // 字段未修改，则不需要修改告警记录
                                if (StringUtils.hasText(before.getName()) &&
                                    before.getName().equals(config.getName()) &&
                                    before.getLevel() != null &&
                                    before.getLevel().equals(config.getLevel())) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .flatMap(this::updateAlarmRecord)
                        .then());
    }

    @EventListener
    public void handleAlarmConfigSaved(EntitySavedEvent<AlarmConfigEntity> event) {
        event.async(Mono.defer(() -> Flux
            .fromIterable(event.getEntity())
            .flatMap(config -> handleAlarmConfigCRU("/_sys/alarm/config/saved", config)
                .then(updateAlarmRecord(config)))
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
    private Mono<Void> handleAlarmConfigCRU(String topic, AlarmConfigEntity config) {
        if (AlarmState.enabled.equals(config.getState())) {
            return doAlarmConfigCRU(config)
                .then(eventBus.publish(topic, config))
                .then();
        } else {
            return handleAlarmConfigDelete(config);
        }
    }

    //处理告警配置删除
    private Mono<Void> handleAlarmConfigDelete(AlarmConfigEntity alarmConfig) {
        return doAlarmConfigDelete(alarmConfig)
            .then(eventBus.publish("/_sys/alarm/config/deleted", alarmConfig))
            .then();
    }


    //告警配置创建、修改、保存
    private Mono<Void> doAlarmConfigCRU(AlarmConfigEntity alarmConfig) {

        if (StringUtils.hasText(alarmConfig.getSceneId())) {
            alarmConfigCache
                .computeIfAbsent(alarmConfig.getSceneId(), (k) -> new ConcurrentHashMap<>())
                .put(alarmConfig.getId(), alarmConfig);
        }

        return Mono.empty();
    }

    //告警配置删除
    private Mono<Void> doAlarmConfigDelete(AlarmConfigEntity alarmConfig) {

        if (StringUtils.hasText(alarmConfig.getSceneId())) {
            alarmConfigCache.compute(alarmConfig.getSceneId(), (k, v) -> {
                if (v != null) {
                    v.remove(alarmConfig.getId());
                    if (v.size() == 0) {
                        return null;
                    }
                }
                return v;
            });
        }

        return Mono.empty();
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
        entity.setSourceId(targetInfo.getSourceId());
        entity.setSourceType(targetInfo.getSourceType());
        entity.setSourceName(targetInfo.getSourceName());

        entity.generateId();
        return entity;
    }

    // 修改告警记录
    private Mono<Integer> updateAlarmRecord(AlarmConfigEntity config) {
        return alarmRecordService
            .createUpdate()
            .set(AlarmRecordEntity::getAlarmName, config.getName())
            .set(AlarmRecordEntity::getLevel, config.getLevel())
            .where(AlarmRecordEntity::getAlarmConfigId, config.getId())
            .execute();
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
