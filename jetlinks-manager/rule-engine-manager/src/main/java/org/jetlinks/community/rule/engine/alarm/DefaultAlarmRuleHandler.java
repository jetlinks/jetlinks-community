package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.RuleEngineConstants;
import org.jetlinks.community.rule.engine.entity.*;
import org.jetlinks.community.rule.engine.enums.AlarmHandleType;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.CompositeSet;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
@Component
public class DefaultAlarmRuleHandler implements AlarmRuleHandler, CommandLineRunner {

    private static final Set<String> configInfoKey = new HashSet<>(
        Arrays.asList(
            AlarmConstants.ConfigKey.alarmConfigId,
            AlarmConstants.ConfigKey.alarmName,
            AlarmConstants.ConfigKey.level,
            AlarmConstants.ConfigKey.targetType,
            AlarmConstants.ConfigKey.state,
            AlarmConstants.ConfigKey.ownerId,
            AlarmConstants.ConfigKey.description
        ));

    private final Map<Tuple2<String, Integer>, Set<String>> ruleAlarmBinds = new ConcurrentHashMap<>();

    private final AlarmRecordService alarmRecordService;
    private final AlarmHistoryService historyService;
    private final ConfigStorageManager storageManager;
    private final ApplicationEventPublisher eventPublisher;

    private final EventBus eventBus;

    public final ReactiveRepository<AlarmRuleBindEntity, String> bindRepository;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;

    public final AlarmConfigService alarmConfigService;

    @Override
    public Flux<Result> triggered(ExecutionContext context, RuleData data) {
        return this
            .parseAlarmInfo(context, data)
            .flatMap(this::triggerAlarm);
    }

    @Override
    public Flux<Result> relieved(ExecutionContext context, RuleData data) {
        return this
            .parseAlarmInfo(context, data)
            .flatMap(this::relieveAlarm);
    }

    private Flux<AlarmInfo> parseAlarmInfo(ExecutionContext context, RuleData data) {
        if (ruleAlarmBinds.isEmpty()) {
            return Flux.empty();
        }
        //节点所在的条件分支索引
        int branchIndex = context
            .getJob()
            .getConfiguration(SceneRule.ACTION_KEY_BRANCH_INDEX)
            .map(idx -> CastUtils.castNumber(idx).intValue())
            .orElse(AlarmRuleBindEntity.ANY_BRANCH_INDEX);

        Set<String> alarmId = getBoundAlarmId(context.getInstanceId(), branchIndex);

        if (CollectionUtils.isEmpty(alarmId)) {
            return Flux.empty();
        }

        Map<String, Object> contextMap = RuleDataHelper.toContextMap(data);
        return Flux
            .fromIterable(alarmId)
            .flatMap(this::getAlarmStorage)
            .flatMap(store -> parseAlarm(context, store, contextMap));
    }

    private Set<String> getBoundAlarmId(String ruleId, int branchIndex) {
        //指定和特定分支绑定的告警
        Set<String> specific = ruleAlarmBinds.get(Tuples.of(ruleId, branchIndex));

        //未指定特定分支的告警
        Set<String> any = ruleAlarmBinds.get(Tuples.of(ruleId, AlarmRuleBindEntity.ANY_BRANCH_INDEX));

        //没有任何告警绑定了规则
        if (CollectionUtils.isEmpty(specific) && CollectionUtils.isEmpty(any)) {
            return Collections.emptySet();
        }

        //只有特定分支
        if (CollectionUtils.isNotEmpty(specific) && CollectionUtils.isEmpty(any)) {
            return specific;
        }
        //只有任意规则
        else if (CollectionUtils.isEmpty(specific) && CollectionUtils.isNotEmpty(any)) {
            return any;
        } else {
            return new CompositeSet<>(specific, any);
        }
    }

    private AlarmRecordEntity ofRecord(Result result) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setAlarmConfigId(result.getAlarmConfigId());
        entity.setState(AlarmRecordState.warning);
        entity.setAlarmTime(System.currentTimeMillis());
        entity.setLevel(result.getLevel());
        entity.setTargetType(result.getTargetType());
        entity.setTargetName(result.getTargetName());
        entity.setTargetId(result.getTargetId());

        entity.setSourceType(result.getSourceType());
        entity.setSourceName(result.getSourceName());
        entity.setSourceId(result.getSourceId());

        entity.setAlarmName(result.getAlarmName());
        entity.setDescription(result.getDescription());
        entity.generateId();
        return entity;
    }

    private Flux<AlarmInfo> parseAlarm(ExecutionContext context, ConfigStorage alarm, Map<String, Object> contextMap) {
        return this
            .getAlarmInfo(alarm)
            .flatMapMany(result -> {

                String ruleName = RuleEngineConstants
                    .getRuleName(context)
                    .orElse(result.getAlarmName());

                AlarmData alarmData = AlarmData.of(
                    result.getAlarmConfigId(),
                    result.getAlarmName(),
                    context.getInstanceId(),
                    ruleName,
                    contextMap);

                result.setData(alarmData);

                return AlarmTarget
                    .of(result.getTargetType())
                    .convert(alarmData)
                    .map(result::copyWith);
            })
            .flatMap(info -> this
                .getRecordCache(info.createRecordId())
                .map(info::with)
                .defaultIfEmpty(info));
    }

    private Mono<AlarmInfo> relieveAlarm(AlarmInfo result) {
        // 已经被解除不重复更新
        if (result.isCached() && !result.isAlarming()) {
            return Mono.empty();
        }

        AlarmRecordEntity record = ofRecord(result);
        return Mono
            .zip(alarmRecordService.changeRecordState(AlarmRecordState.normal, record.getId()),
                 updateRecordCache(record.getId(), RecordCache::withNormal),
                 (total, ignore) -> total)
            .flatMap(total -> {
                //如果有数据被更新说明是正在告警中
                if (total > 0) {
                    result.setAlarming(true);
                    return saveAlarmHandleHistory(record);
                }
                return Mono.empty();
            })
            .thenReturn(result);
    }

    private Mono<Void> saveAlarmHandleHistory(AlarmRecordEntity record) {
        AlarmHandleInfo alarmHandleInfo = new AlarmHandleInfo();
        alarmHandleInfo.setHandleTime(System.currentTimeMillis());
        alarmHandleInfo.setAlarmRecordId(record.getId());
        alarmHandleInfo.setAlarmConfigId(record.getAlarmConfigId());
        alarmHandleInfo.setAlarmTime(record.getAlarmTime());
        alarmHandleInfo.setState(AlarmRecordState.normal);
        alarmHandleInfo.setType(AlarmHandleType.system);
        alarmHandleInfo.setDescribe(LocaleUtils.resolveMessage("message.scene_triggered_relieve_alarm", "场景触发解除告警"));
        // TODO: 2022/12/22 批量缓冲保存
        return handleHistoryRepository
            .save(AlarmHandleHistoryEntity.of(alarmHandleInfo))
            .then();
    }


    private Mono<AlarmInfo> triggerAlarm(AlarmInfo result) {
        AlarmRecordEntity record = ofRecord(result);

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
                AlarmHistoryInfo historyInfo = createHistory(record, result);
                result.setAlarmTime(record.getAlarmTime());

                //更新结果返回0 说明是新产生的告警数据
                if (total == 0) {
                    result.setFirstAlarm(true);
                    result.setAlarming(false);

                    return alarmRecordService
                        .save(record)
                        .then(historyService.save(historyInfo))
                        .then(publishAlarmRecord(historyInfo, result))
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
    }

    private Mono<Void> publishEvent(AlarmHistoryInfo historyInfo) {
        return Mono.fromRunnable(() -> eventPublisher.publishEvent(historyInfo));
    }

    private AlarmHistoryInfo createHistory(AlarmRecordEntity record, AlarmInfo alarmInfo) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setId(IDGenerator.RANDOM.generate());
        info.setAlarmConfigId(record.getAlarmConfigId());
        info.setAlarmConfigName(record.getAlarmName());
        info.setDescription(record.getDescription());
        info.setAlarmRecordId(record.getId());
        info.setLevel(record.getLevel());
        info.setAlarmTime(record.getAlarmTime());

        info.setTargetName(record.getTargetName());
        info.setTargetId(record.getTargetId());
        info.setTargetType(record.getTargetType());

        info.setSourceType(record.getSourceType());
        info.setSourceName(record.getSourceName());
        info.setSourceId(record.getSourceId());


        info.setAlarmInfo(ObjectMappers.toJsonString(alarmInfo.getData().getOutput()));
        return info;
    }

    public Mono<Void> publishAlarmRecord(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarm(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());
        return eventBus
            .publish(topic, historyInfo)
            .then();
    }

    private Mono<AlarmInfo> saveAlarmCache(AlarmInfo result,
                                           AlarmRecordEntity record) {

        return this
            .updateRecordCache(record.getId(), cache -> cache.with(result))
            .thenReturn(result);

//        return this
//            .getAlarmStorage(result.getAlarmConfigId())
//            .flatMap(store -> {
//                Map<String, Object> configs = new HashMap<>();
//
//                configs.put(AlarmConstants.ConfigKey.lastAlarmTime, record.getAlarmTime());
//                if (!result.isAlarming()) {
//                    configs.put(AlarmConstants.ConfigKey.alarmTime, record.getAlarmTime());
//                }
//                return store.setConfigs(configs);
//            })
//            .thenReturn(result);
    }

    private Mono<AlarmInfo> getAlarmInfo(ConfigStorage alarm) {
        return alarm
            .getConfigs(configInfoKey)
            .mapNotNull(values -> {
                //告警禁用了
                if (values
                    .getString(AlarmConstants.ConfigKey.state, AlarmState.enabled.name())
                    .equals(AlarmState.disabled.name())) {
                    return null;
                }

                AlarmInfo result = FastBeanCopier.copy(values.getAllValues(), new AlarmInfo());

                if (result.getAlarmConfigId() == null ||
                    result.getAlarmName() == null) {
                    //缓存丢失了?从数据库里获取?
                    return null;
                }

                return result;
            });
    }

    private Mono<ConfigStorage> getAlarmStorage(String alarmId) {
        return storageManager.getStorage("alarm:" + alarmId);
    }


    /*  处理告警配置缓存事件 */

    static final String TOPIC_ALARM_CONFIG_SAVE = "/_sys/device-alarm-config/save";
    static final String TOPIC_ALARM_CONFIG_DELETE = "/_sys/device-alarm-rule/del";

    @EventListener
    public void handleConfigEvent(EntitySavedEvent<AlarmConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_CONFIG_SAVE, e))
        );
    }

    @EventListener
    public void handleConfigEvent(EntityCreatedEvent<AlarmConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_CONFIG_SAVE, e))
        );
    }

    @EventListener
    public void handleConfigEvent(EntityModifyEvent<AlarmConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_CONFIG_SAVE, e))
        );
    }

    @EventListener
    public void handleConfigEvent(EntityDeletedEvent<AlarmConfigEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_CONFIG_DELETE, e))
        );
    }


    @Subscribe(value = TOPIC_ALARM_CONFIG_SAVE, features = {Subscription.Feature.local, Subscription.Feature.broker})
    public Mono<Void> handleAlarmConfig(AlarmConfigEntity entity) {
        return this
            .getAlarmStorage(entity.getId())
            .flatMap(store -> store.setConfigs(entity.toConfigMap()))
            .then();
    }

    @Subscribe(value = TOPIC_ALARM_CONFIG_DELETE, features = {Subscription.Feature.local, Subscription.Feature.broker})
    public Mono<Void> removeAlarmConfig(AlarmConfigEntity entity) {
        return this
            .getAlarmStorage(entity.getId())
            .flatMap(ConfigStorage::clear)
            .then();
    }


    /*  处理告警和规则绑定事件 */
    static final String TOPIC_ALARM_RULE_BIND = "/_sys/device-alarm-rule/bind";
    static final String TOPIC_ALARM_RULE_UNBIND = "/_sys/device-alarm-rule/unbind";


    @EventListener
    public void handleBindEvent(EntitySavedEvent<AlarmRuleBindEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_RULE_BIND, e))
        );
    }

    @EventListener
    public void handleBindEvent(EntityCreatedEvent<AlarmRuleBindEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_RULE_BIND, e))
        );
    }

    @EventListener
    public void handleBindEvent(EntityDeletedEvent<AlarmRuleBindEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(e -> eventBus.publish(TOPIC_ALARM_RULE_UNBIND, e))
        );
    }

    @Subscribe(value = TOPIC_ALARM_RULE_UNBIND, features = {Subscription.Feature.local, Subscription.Feature.broker})
    public void handleUnBind(AlarmRuleBindEntity entity) {
        Integer index = entity.getBranchIndex();
        if (index == null) {
            index = AlarmRuleBindEntity.ANY_BRANCH_INDEX;
        }

        ruleAlarmBinds
            .compute(Tuples.of(entity.getRuleId(), index), (key, value) -> {
                if (value == null) {
                    return null;
                }
                value.remove(entity.getAlarmId());
                if (value.isEmpty()) {
                    return null;
                }
                return value;
            });
    }

    @Subscribe(value = TOPIC_ALARM_RULE_BIND, features = {Subscription.Feature.local, Subscription.Feature.broker})
    public void handleBind(AlarmRuleBindEntity entity) {
        Integer index = entity.getBranchIndex();
        if (index == null) {
            index = AlarmRuleBindEntity.ANY_BRANCH_INDEX;
        }
        ruleAlarmBinds
            .computeIfAbsent(Tuples.of(entity.getRuleId(), index), ignore -> ConcurrentHashMap.newKeySet())
            .add(entity.getAlarmId());
    }

    @Override
    public void run(String... args) throws Exception {
        //启动时加载绑定配置
        bindRepository
            .createQuery()
            .fetch()
            .doOnNext(this::handleBind)
            //加载告警配置数据到缓存
            .thenMany(alarmConfigService
                          .createQuery()
                          .fetch()
                          .doOnNext(this::handleAlarmConfig)
            )
            .subscribe();

    }

    @Getter
    @Setter
    public static class AlarmInfo extends Result {
        /**
         * 告警所有者用户ID,表示告警是属于哪个用户的,用于进行数据权限控制
         */
        private String ownerId;

        private AlarmData data;

        private boolean cached;

        @Override
        public AlarmInfo copyWith(AlarmTargetInfo targetInfo) {
            AlarmInfo result = FastBeanCopier.copy(this, new AlarmInfo());
            result.setTargetType(targetInfo.getTargetType());
            result.setTargetId(targetInfo.getTargetId());
            result.setTargetName(targetInfo.getTargetName());

            result.setSourceId(targetInfo.getSourceId());
            result.setSourceType(targetInfo.getSourceType());
            result.setSourceName(targetInfo.getSourceName());

            return result;
        }

        public AlarmInfo with(RecordCache cache) {
            this.setAlarmTime(cache.alarmTime);
            this.setLastAlarmTime(cache.lastAlarmTime);
            this.setAlarming(cache.isAlarming());
            this.cached = true;
            return this;
        }

        public String createRecordId() {
            return AlarmRecordEntity.generateId(getTargetId(), getTargetType(), getAlarmConfigId());
        }
    }


    private Mono<RecordCache> getRecordCache(String recordId) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(RecordCache.class)));
    }

    private Mono<RecordCache> updateRecordCache(String recordId, Function<RecordCache, RecordCache> handler) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(RecordCache.class))
                .switchIfEmpty(Mono.fromSupplier(RecordCache::new))
                .mapNotNull(handler)
                .flatMap(cache -> store.setConfig(recordId, cache)
                                       .thenReturn(cache)));
    }

    public static class RecordCache implements Externalizable {

        static final byte stateNormal = 0x01;
        static final byte stateAlarming = 0x02;

        byte state;
        long alarmTime;
        long lastAlarmTime;


        public boolean isAlarming() {
            return state == stateAlarming;
        }

        public RecordCache withNormal() {
            this.state = stateNormal;
            return this;
        }

        public RecordCache withAlarming() {
            this.state = stateAlarming;
            return this;
        }

        public RecordCache with(Result record) {

            this.lastAlarmTime = this.alarmTime == 0 ? record.getAlarmTime() : this.alarmTime;

            this.alarmTime = record.getAlarmTime();

            if (record.isAlarming() || record.isFirstAlarm()) {

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

}
