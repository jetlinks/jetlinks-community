package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.command.rule.data.RelieveResult;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.community.utils.ObjectMappers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Component
public class DefaultAlarmHandler implements AlarmHandler {

    private final AlarmRecordService alarmRecordService;

    private final AlarmHistoryService historyService;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;

    private final EventBus eventBus;

    private final ConfigStorageManager storageManager;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<AlarmResult> triggerAlarm(AlarmInfo alarmInfo) {
        return getRecordCache(createRecordId(alarmInfo))
            .map(this::ofRecordCache)
            .defaultIfEmpty(new AlarmResult())
            .flatMap(result -> {
                AlarmRecordEntity record = ofRecord(result, alarmInfo);
                //更新告警状态.
                return alarmRecordService
                    .createUpdate()
                    .set(record)
                    .where(AlarmRecordEntity::getId, record.getId())
                    .and(AlarmRecordEntity::getState, AlarmRecordState.warning)
                    .execute()
                    //更新数据库报错,依然尝试触发告警!
                    .onErrorResume(err -> {
                        log.error("trigger alarm error", err);
                        return Reactors.ALWAYS_ZERO;
                    })
                    .flatMap(total -> {
                        AlarmHistoryInfo historyInfo = createHistory(record, alarmInfo);

                        //更新结果返回0 说明是新产生的告警数据
                        if (total == 0) {
                            result.setFirstAlarm(true);
                            result.setAlarming(false);
                            result.setAlarmTime(historyInfo.getAlarmTime());
                            record.setAlarmTime(historyInfo.getAlarmTime());
                            record.setHandleTime(null);
                            record.setHandleType(null);

                            return this
                                .saveAlarmRecord(record)
                                .then(historyService.save(historyInfo))
                                .then(publishAlarmRecord(historyInfo, alarmInfo))
                                .then(publishEvent(historyInfo))
                                .then(saveAlarmCache(result, record));
                        }
                        result.setFirstAlarm(false);
                        result.setAlarming(true);
                        return historyService
                            .save(historyInfo)
                            .then(publishEvent(historyInfo))
                            .then(saveAlarmCache(result, record));
                    });
            });

    }

    private Mono<Void> saveAlarmRecord(AlarmRecordEntity record){
        return alarmRecordService
            .createUpdate()
            .set(record)
            .setNull(AlarmRecordEntity::getHandleTime)
            .setNull(AlarmRecordEntity::getHandleType)
            .where(AlarmRecordEntity::getId, record.getId())
            .execute()
            .flatMap(update -> {
                // 如果是首次告警需要手动保存
                if (update == 0) {
                    return alarmRecordService.save(record).then();
                }
                return Mono.empty();
            });
    }

    @Override
    public Mono<RelieveResult> relieveAlarm(RelieveInfo relieveInfo) {
        return getRecordCache(createRecordId(relieveInfo))
            .map(this::ofRecordCache)
            .defaultIfEmpty(new AlarmResult())
            .flatMap(result -> {
                AlarmRecordEntity record = this.ofRecord(result, relieveInfo);
                return Mono
                    .zip(alarmRecordService.changeRecordState(
                             relieveInfo.getAlarmRelieveType()
                             , AlarmRecordState.normal,
                             record.getId()),
                         this.updateRecordCache(record.getId(), DefaultAlarmRuleHandler.RecordCache::withNormal),
                         (total, ignore) -> total)
                    .flatMap(total -> {
                        //如果有数据被更新说明是正在告警中
                        if (total > 0) {
                            result.setAlarming(true);
                            AlarmHistoryInfo historyInfo = this.createHistory(record, relieveInfo);
                            RelieveResult relieveResult = FastBeanCopier.copy(record, new RelieveResult());
                            relieveResult.setRelieveTime(System.currentTimeMillis());
                            relieveResult.setActualDesc(historyInfo.getActualDesc());
                            relieveResult.setRelieveReason(relieveInfo.getRelieveReason());
                            relieveResult.setDescribe(relieveInfo.getDescription());
                            return saveAlarmHandleHistory(relieveInfo, record)
                                .then(publishAlarmRelieve(historyInfo, relieveInfo))
                                .thenReturn(relieveResult);
                        }
                        return Mono.empty();
                    });
            });
    }

    public AlarmRecordEntity ofRecord(AlarmResult result, AlarmInfo alarmData) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setAlarmConfigId(alarmData.getAlarmConfigId());
        entity.setAlarmTime(result.getAlarmTime());
        entity.setState(AlarmRecordState.warning);
        entity.setLevel(alarmData.getLevel());
        entity.setTargetType(alarmData.getTargetType());
        entity.setTargetName(alarmData.getTargetName());
        entity.setTargetId(alarmData.getTargetId());

        entity.setSourceType(alarmData.getSourceType());
        entity.setSourceName(alarmData.getSourceName());
        entity.setSourceId(alarmData.getSourceId());

        entity.setAlarmName(alarmData.getAlarmName());
        entity.setDescription(alarmData.getDescription());
        entity.setAlarmConfigSource(alarmData.getAlarmConfigSource());
        if (alarmData.getTermSpec() != null) {
            entity.setTermSpec(alarmData.getTermSpec());
            entity.setTriggerDesc(alarmData.getTermSpec().getTriggerDesc());
            entity.setActualDesc(alarmData.getTermSpec().getActualDesc());
        }
        entity.generateId();
        return entity;
    }

    public AlarmHistoryInfo createHistory(AlarmRecordEntity record, AlarmInfo alarmInfo) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setId(IDGenerator.RANDOM.generate());
        info.setAlarmConfigId(record.getAlarmConfigId());
        info.setAlarmConfigName(record.getAlarmName());
        info.setDescription(record.getDescription());
        info.setAlarmRecordId(record.getId());
        info.setLevel(record.getLevel());
        info.setAlarmTime(System.currentTimeMillis());
        info.setTriggerDesc(record.getTriggerDesc());
        info.setAlarmConfigSource(alarmInfo.getAlarmConfigSource());
        info.setActualDesc(record.getActualDesc());

        info.setTargetName(record.getTargetName());
        info.setTargetId(record.getTargetId());
        info.setTargetType(record.getTargetType());

        info.setSourceType(record.getSourceType());
        info.setSourceName(record.getSourceName());
        info.setSourceId(record.getSourceId());

        info.setAlarmInfo(ObjectMappers.toJsonString(alarmInfo.getData()));
        return info;
    }

    public Mono<Void> publishAlarmRecord(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarm(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return doPublishAlarmHistoryInfo(topic, historyInfo, alarmInfo);
    }

    public Mono<Void> doPublishAlarmHistoryInfo(String topic, AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        return Mono.just(topic)
            .flatMap(assetTopic -> eventBus.publish(assetTopic, historyInfo))
            .then();
    }

    private Mono<Void> publishEvent(AlarmHistoryInfo historyInfo) {
        return Mono.fromRunnable(() -> eventPublisher.publishEvent(historyInfo));
    }

    private Mono<AlarmResult> saveAlarmCache(AlarmResult result,
                                             AlarmRecordEntity record) {
        return this
            .updateRecordCache(record.getId(), cache -> cache.with(result))
            .thenReturn(result);
    }

    public Mono<Void> publishAlarmRelieve(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarmRelieve(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return this.doPublishAlarmHistoryInfo(topic, historyInfo, alarmInfo);
    }

    private Mono<Void> saveAlarmHandleHistory(RelieveInfo relieveInfo, AlarmRecordEntity record) {
        AlarmHandleInfo alarmHandleInfo = new AlarmHandleInfo();
        alarmHandleInfo.setHandleTime(System.currentTimeMillis());
        alarmHandleInfo.setAlarmRecordId(record.getId());
        alarmHandleInfo.setAlarmConfigId(record.getAlarmConfigId());
        alarmHandleInfo.setAlarmTime(record.getAlarmTime());
        alarmHandleInfo.setState(AlarmRecordState.normal);
        alarmHandleInfo.setType(relieveInfo.getAlarmRelieveType());
        alarmHandleInfo.setDescribe(getLocaleDescribe());
        // TODO: 2022/12/22 批量缓冲保存
        return handleHistoryRepository
            .save(AlarmHandleHistoryEntity.of(alarmHandleInfo))
            .then();
    }

    private String getLocaleDescribe() {
        return LocaleUtils.resolveMessage("message.scene_triggered_relieve_alarm", "场景触发解除告警");
    }


    private String createRecordId(AlarmInfo alarmInfo) {
        return AlarmRecordEntity.generateId(alarmInfo.getTargetId(), alarmInfo.getTargetType(), alarmInfo.getAlarmConfigId());
    }

    private Mono<DefaultAlarmRuleHandler.RecordCache> getRecordCache(String recordId) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(DefaultAlarmRuleHandler.RecordCache.class)));
    }

    private Mono<DefaultAlarmRuleHandler.RecordCache> updateRecordCache(String recordId, Function<DefaultAlarmRuleHandler.RecordCache, DefaultAlarmRuleHandler.RecordCache> handler) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(DefaultAlarmRuleHandler.RecordCache.class))
                .switchIfEmpty(Mono.fromSupplier(DefaultAlarmRuleHandler.RecordCache::new))
                .mapNotNull(handler)
                .flatMap(cache -> store.setConfig(recordId, cache)
                                       .thenReturn(cache)));
    }


    private AlarmResult ofRecordCache(DefaultAlarmRuleHandler.RecordCache cache) {
        AlarmResult result = new AlarmResult();
        result.setAlarmTime(cache.alarmTime);
        result.setLastAlarmTime(cache.lastAlarmTime);
        result.setAlarming(cache.isAlarming());
        return result;
    }
}
