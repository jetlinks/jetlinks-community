package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.command.rule.data.RelieveResult;
import org.jetlinks.community.lock.ReactiveLock;
import org.jetlinks.community.lock.ReactiveLockHolder;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.enums.AlarmHandleState;
import org.jetlinks.community.rule.engine.enums.AlarmHandleType;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.utils.TopicUtils;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.utils.Reactors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@EnableConfigurationProperties(AlarmProperties.class)
public class DefaultAlarmHandler implements AlarmHandler {

    static final String CACHE_ID = "alarm-records";

    private final AlarmRecordService alarmRecordService;

    private final AlarmHistoryService historyService;

    private final AlarmHandleHistoryService alarmHandleHistoryService;

    private final EventBus eventBus;

    private final ConfigStorageManager storageManager;

    private final ApplicationEventPublisher eventPublisher;

    private final AlarmProperties alarmProperties;

    public DefaultAlarmHandler(AlarmRecordService alarmRecordService,
                               AlarmHistoryService historyService,
                               AlarmHandleHistoryService alarmHandleHistoryService,
                               EventBus eventBus,
                               ConfigStorageManager storageManager,
                               ApplicationEventPublisher eventPublisher,
                               AlarmProperties alarmProperties) {
        this.alarmRecordService = alarmRecordService;
        this.historyService = historyService;
        this.alarmHandleHistoryService = alarmHandleHistoryService;
        this.eventBus = eventBus;
        this.storageManager = storageManager;
        this.eventPublisher = eventPublisher;
        this.alarmProperties = alarmProperties;
    }

    @Override
    public Mono<AlarmResult> triggerAlarm(AlarmInfo alarmInfo) {
        String recordId = createRecordId(alarmInfo);
        ReactiveLock lock = ReactiveLockHolder
            .getLock("triggerAlarm:" + recordId);
        return this
            .getRecordCache(recordId)
            .flatMap(cache -> {
                if (alarmInfo.getAlarmTime() == null) {
                    alarmInfo.setAlarmTime(System.currentTimeMillis());
                }
                AlarmRecordEntity record = ofRecord(cache, alarmInfo);
                AlarmResult result = ofRecordCache(cache);
                AlarmHistoryInfo historyInfo = createHistory(record, alarmInfo);
                //时间滞后的告警数据，仅记录日志.
                if (!cache.isEffectiveTrigger(alarmInfo.getAlarmTime())) {
                    return historyService
                        .save(historyInfo)
                        .thenReturn(result);
                }
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
                                //初始化告警处理记录
                                .then(Mono.defer(() -> {
                                    AlarmHandleInfo alarmHandleInfo = createAlarmHandleInfo(record);
                                    if (alarmProperties.getHandleHistory().isCreateWhenAlarm()) {
                                        return alarmHandleHistoryService
                                            .save(AlarmHandleHistoryEntity.of(alarmHandleInfo))
                                            .then(publishAlarmHandleHistory(alarmInfo, alarmHandleInfo));
                                    }
                                    return Mono.empty();
                                }))
                                .then(publishAlarmLog(historyInfo, alarmInfo))
                                .then(saveAlarmCache(result, record));
                        }
                        result.setFirstAlarm(false);
                        result.setAlarming(true);
                        result.setAlarmTime(historyInfo.getAlarmTime());
                        return historyService
                            .save(historyInfo)
                            .then(publishAlarmLog(historyInfo, alarmInfo))
                            .then(saveAlarmCache(result, record));
                    });
            })
            .as(lock::lock);
    }

    @Override
    public Mono<RelieveResult> relieveAlarm(RelieveInfo relieveInfo) {
        return this
            .getRecordCache(createRecordId(relieveInfo))
            .flatMap(cache -> {
                if (relieveInfo.getRelieveTime() == null) {
                    relieveInfo.setRelieveTime(System.currentTimeMillis());
                }
                long relieveTime = relieveInfo.getRelieveTime();
                AlarmRecordEntity record = this.ofRecord(cache, relieveInfo);
                AlarmHistoryInfo historyInfo = this.createHistory(record, relieveInfo);
                RelieveResult relieveResult = createRelieveResult(record, relieveInfo);
                //无效解除告警,更新解除告警时间
                if (!cache.isEffectiveRelieve(relieveInfo.getRelieveTime())) {
                    return this.updateRecordCache(
                            record.getId(),
                            relieveTime,
                            _cache -> _cache.withRelievedTime(relieveTime),
                            false, true)
                        .thenReturn(relieveResult);
                }
                return Mono
                    .zip(alarmRecordService.changeRecordState(
                            StringUtils.hasText(relieveInfo.getAlarmRelieveType())
                                ? relieveInfo.getAlarmRelieveType()
                                : AlarmHandleType.system.getValue(),
                            AlarmRecordState.normal,
                            record.getId()),
                        this.updateRecordCache(record.getId(),
                            relieveTime,
                            _cache -> _cache.relieved(relieveTime),
                            false, true),
                        (total, ignore) -> total)
                    .flatMap(total -> {
                        //如果有数据被更新说明是正在告警中
                        if (total > 0) {
                            AlarmHandleInfo alarmHandleInfo = createRelieveAlarmHandleInfo(relieveInfo, record);
                            return alarmHandleHistoryService
                                .save(AlarmHandleHistoryEntity.of(alarmHandleInfo))
                                .then(publishAlarmHandleHistory(relieveInfo, alarmHandleInfo))
                                .then(publishAlarmRelieve(historyInfo, relieveInfo))
                                .thenReturn(relieveResult);
                        }
                        return Mono.empty();
                    });
            });
    }

    public Mono<Void> publishAlarmHandleHistory(AlarmInfo alarmInfo, AlarmHandleInfo info) {
        String topic = Topics.alarmHandleHistory(info.getTargetType(), info.getTargetId(), info.getAlarmConfigId());
        Map<String, Object> configs = new HashMap<>();
        configs.put(PropertyConstants.creatorId.getKey(), info.getRecordCreatorId());

        return Flux
            .fromIterable(TopicUtils.refactorTopic(configs, topic))
            .flatMap(assetTopic -> eventBus.publish(assetTopic, info))
            .then();
    }

    public Mono<Void> publishAlarmRecord(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarm(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return doPublishAlarmInfo(topic, historyInfo, alarmInfo);
    }

    public Mono<Void> doPublishAlarmInfo(String topic, AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
//        Set<String> userId = new HashSet<>();
//        for (Map<String, Object> binding : alarmInfo.getBindings()) {
//            if (DefaultDimensionType.user.getId().equals(binding.get("type"))) {
//                userId.add(binding.get("id").toString());
//            }
//        }

        Map<String, Object> configs = new HashMap<>();
        configs.put(PropertyConstants.creatorId.getKey(), historyInfo.getCreatorId());

        return Flux
            .fromIterable(TopicUtils.refactorTopic(configs, topic))
            .flatMap(assetTopic -> eventBus.publish(assetTopic, historyInfo))
            .then();
    }


    public Mono<Void> publishAlarmRelieve(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarmRelieve(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return this.doPublishAlarmInfo(topic, historyInfo, alarmInfo);
    }

    protected AlarmHistoryInfo createHistory(AlarmRecordEntity record, AlarmInfo alarmInfo) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setId(IDGenerator.RANDOM.generate());
        info.setAlarmConfigId(record.getAlarmConfigId());
        info.setAlarmConfigName(record.getAlarmName());
        info.setDescription(record.getDescription());
        info.setAlarmRecordId(record.getId());
        info.setLevel(record.getLevel());
        info.setAlarmTime(alarmInfo.getAlarmTime());
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
        info.setCreatorId(record.getCreatorId());
        return info;
    }

    private RelieveResult createRelieveResult(AlarmRecordEntity record, RelieveInfo relieveInfo) {
        AlarmHistoryInfo historyInfo = this.createHistory(record, relieveInfo);
        RelieveResult relieveResult = FastBeanCopier.copy(record, new RelieveResult());
        relieveResult.setRelieveTime(relieveInfo.getRelieveTime());
        relieveResult.setActualDesc(historyInfo.getActualDesc());
        relieveResult.setRelieveReason(relieveInfo.getRelieveReason());
        relieveResult.setDescribe(relieveInfo.getDescription());
        return relieveResult;
    }

    private Mono<Void> saveAlarmRecord(AlarmRecordEntity record) {
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

    private AlarmRecordEntity ofRecord(RecordCache cache, AlarmInfo alarmData) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setAlarmConfigId(alarmData.getAlarmConfigId());
        entity.setAlarmTime(cache.getTrigger().alarmTime);
        entity.setLastAlarmTime(alarmData.getAlarmTime());
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
        entity.setCreatorId(alarmData.getSourceCreatorId());
        if (alarmData.getTermSpec() != null) {
            entity.setTermSpec(alarmData.getTermSpec());
            entity.setTriggerDesc(alarmData.getTermSpec().getTriggerDesc());
            entity.setActualDesc(alarmData.getTermSpec().getActualDesc());
        }
        entity.generateId();
        return entity;
    }

    private Mono<Void> publishAlarmLog(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarmLog(historyInfo.getTargetType(),
            historyInfo.getTargetId(),
            historyInfo.getAlarmConfigId(),
            historyInfo.getAlarmRecordId());

        eventPublisher.publishEvent(historyInfo);
        return doPublishAlarmInfo(topic, historyInfo, alarmInfo);
    }

    private Mono<AlarmResult> saveAlarmCache(AlarmResult result,
                                             AlarmRecordEntity record) {
        return this
            .updateRecordCache(record.getId(),
                result.getAlarmTime(),
                cache -> cache.with(result),
                true, false)
            .thenReturn(result);
    }


    private AlarmHandleInfo createRelieveAlarmHandleInfo(RelieveInfo relieveInfo, AlarmRecordEntity record) {
        AlarmHandleInfo alarmHandleInfo = createAlarmHandleInfo(record);
        alarmHandleInfo.setHandleTime(relieveInfo.getRelieveTime() == null
            ? System.currentTimeMillis() : relieveInfo.getRelieveTime());
        alarmHandleInfo.setState(AlarmRecordState.normal);
        alarmHandleInfo.setType(relieveInfo.getAlarmRelieveType() == null ?
            AlarmHandleType.system.getValue() : relieveInfo.getAlarmRelieveType());
        alarmHandleInfo.setDescribe(StringUtils.hasText(relieveInfo.getDescribe())
            ? relieveInfo.getDescribe()
            : getLocaleDescribe());
        alarmHandleInfo.setHandleState(AlarmHandleState.processed);
        return alarmHandleInfo;
    }


    private AlarmHandleInfo createAlarmHandleInfo(AlarmRecordEntity record) {
        AlarmHandleInfo alarmHandleInfo = FastBeanCopier.copy(record, new AlarmHandleInfo());
        alarmHandleInfo.setAlarmRecordId(record.getId());
        alarmHandleInfo.setState(AlarmRecordState.warning);
        alarmHandleInfo.setRecordCreatorId(record.getCreatorId());
        return alarmHandleInfo;
    }

    private String getLocaleDescribe() {
        return LocaleUtils.resolveMessage("message.scene_triggered_relieve_alarm", "场景触发解除告警");
    }


    private String createRecordId(AlarmInfo alarmInfo) {
        return AlarmRecordEntity.generateId(alarmInfo.getTargetId(), alarmInfo.getTargetType(), alarmInfo.getAlarmConfigId());
    }

    private Mono<RecordCache> getRecordCache(String recordId) {
        return storageManager
            .getStorage(CACHE_ID)
            .flatMap(store -> getCache0(store, recordId));
    }

    Mono<RecordCache> getCache0(ConfigStorage storage, String recordId) {
        String reliveId = recordId + ":relieve";
        return storage
            .getConfigs(recordId, reliveId)
            .map(caches -> new RecordCache(
                caches
                    .getValue(recordId)
                    .map(val -> val.as(TriggerCache.class))
                    .orElseGet(TriggerCache::new),
                caches
                    .getValue(reliveId)
                    .map(val -> val.as(RelieveCache.class))
                    .orElseGet(RelieveCache::new)
            ));
    }

    private Mono<RecordCache> updateRecordCache(
        String recordId,
        Long timestamp,
        Function<RecordCache, RecordCache> handler,
        boolean includeTrigger,
        boolean includeRelieve) {
        String reliveId = recordId + ":relieve";
        return storageManager
            .getStorage(CACHE_ID)
            .flatMap(store -> getCache0(store, recordId)
                .filter(cache -> includeTrigger ? cache.isEffectiveTrigger(timestamp) : cache.isEffectiveTime(timestamp))
                .map(handler)
                .flatMap(cache -> {
                    Map<String, Object> configs = new HashMap<>(2);
                    if (includeTrigger) {
                        configs.put(recordId, cache.getTrigger());
                    }
                    if (includeRelieve) {
                        configs.put(reliveId, cache.getRelieve());
                    }
                    return store
                        .setConfigs(configs)
                        .thenReturn(cache);
                }));
    }

    @Getter
    @AllArgsConstructor
    static class RecordCache {
        private TriggerCache trigger;

        private RelieveCache relieve;

        public RecordCache withNormal() {
            trigger.withNormal();
            return this;
        }

        public RecordCache relieved(long time) {
            trigger.alarmTime = 0;
            relieve.reliveTime = time;
            return withNormal();
        }

        public RecordCache withRelievedTime(long time) {
            relieve.reliveTime = time;
            return this;
        }

        public RecordCache with(AlarmResult result) {
            //触发的时间滞后了?
            if (!isEffectiveTrigger(result.getAlarmTime())) {
                return this;
            }
            trigger.with(result);
            return this;
        }

        //告警在解除最近一次告警之后才算有效
        public boolean isEffectiveTrigger(long timestamp) {
            //只获取时间不可行或者告警仍在运行也需要解除
            return timestamp > relieve.reliveTime || !trigger.isAlarming();
        }

        //解除告警在最近一次告警时间之后   并且  当前告警正在告警中才算有效
        public boolean isEffectiveRelieve(long timestamp) {
            return timestamp > trigger.alarmTime && trigger.isAlarming();
        }

        //有效的触发时间
        public boolean isEffectiveTime(long timestamp) {
            return timestamp > relieve.reliveTime && timestamp > trigger.alarmTime;
        }
    }

    @Getter
    @Setter
    public static class TriggerCache implements Externalizable {

        static final byte stateNormal = 0x01;
        static final byte stateAlarming = 0x02;

        byte state;
        long alarmTime;
        long lastAlarmTime;


        public boolean isAlarming() {
            return state == stateAlarming;
        }

        public TriggerCache withNormal() {
            this.state = stateNormal;
            return this;
        }

        public TriggerCache withAlarming() {
            this.state = stateAlarming;
            return this;
        }

        public TriggerCache with(AlarmResult result) {

            if (result.isFirstAlarm()) {
                this.alarmTime = result.getAlarmTime();
            } else {
                this.alarmTime = this.alarmTime == 0 ? result.getAlarmTime() : this.alarmTime;
            }

            this.lastAlarmTime = result.getAlarmTime();

            if (result.isAlarming() || result.isFirstAlarm()) {

                this.state = stateAlarming;

            } else {
                this.state = stateNormal;
            }
            return this;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(state);
            out.writeLong(alarmTime);
            out.writeLong(lastAlarmTime);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            state = in.readByte();
            alarmTime = in.readLong();
            lastAlarmTime = in.readLong();
        }
    }

    @Getter
    @Setter
    public static class RelieveCache implements Externalizable {
        private long reliveTime;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(reliveTime);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            reliveTime = in.readLong();
        }
    }

    private AlarmResult ofRecordCache(RecordCache cache) {
        AlarmResult result = new AlarmResult();
        result.setAlarmTime(cache.trigger.alarmTime);
        result.setLastAlarmTime(cache.trigger.lastAlarmTime);
        result.setAlarming(cache.trigger.isAlarming());
        return result;
    }

}