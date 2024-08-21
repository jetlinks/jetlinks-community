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
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.CompositeSet;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.rule.engine.RuleEngineConstants;
import org.jetlinks.community.rule.engine.entity.AlarmConfigEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRuleBindEntity;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.springframework.boot.CommandLineRunner;
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
    private final ConfigStorageManager storageManager;

    private final EventBus eventBus;

    public final ReactiveRepository<AlarmRuleBindEntity, String> bindRepository;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;

    public final AlarmConfigService alarmConfigService;

    public final AlarmHandler alarmHandler;

    @Override
    public Flux<Result> triggered(ExecutionContext context, RuleData data) {
        return this
            .parseAlarmInfo(context, data)
            .flatMap(alarmInfo -> alarmHandler
                .triggerAlarm(FastBeanCopier
                                  .copy(alarmInfo, new org.jetlinks.community.command.rule.data.AlarmInfo()))
                .map(info -> {
                    Result result = FastBeanCopier.copy(alarmInfo, new Result());
                    FastBeanCopier.copy(info, result);
                    return result;
                }));
    }

    @Override
    public Flux<Result> relieved(ExecutionContext context, RuleData data) {
        return this
            .parseAlarmInfo(context, data)
            .flatMap(alarmInfo -> {
                // 已经被解除不重复更新
                if (alarmInfo.isCached() && !alarmInfo.isAlarming()) {
                    return Mono.empty();
                }
                return alarmHandler
                    .relieveAlarm(FastBeanCopier.copy(alarmInfo, new RelieveInfo()))
                    .map(info -> {
                        Result result = FastBeanCopier.copy(alarmInfo, new Result());
                        FastBeanCopier.copy(info, result);
                        return result;
                    });
            });
    }

    private Flux<AlarmInfo> parseAlarmInfo(ExecutionContext context, RuleData data) {
        if (ruleAlarmBinds.isEmpty()) {
            return Flux.empty();
        }

        //节点所在的执行动作索引
        int actionIndex = context
            .getJob()
            .getConfiguration(SceneRule.ACTION_KEY_ACTION_ID)
            .map(idx -> CastUtils.castNumber(idx).intValue())
            .orElse(AlarmRuleBindEntity.ANY_BRANCH_INDEX);

        Set<String> alarmId = getBoundAlarmId(context.getInstanceId(), actionIndex);

        if (CollectionUtils.isEmpty(alarmId)) {
            //节点所在的条件分支索引
            int branchIndex = context
                .getJob()
                .getConfiguration(SceneRule.ACTION_KEY_BRANCH_ID)
                .map(idx -> CastUtils.castNumber(idx).intValue())
                .orElse(AlarmRuleBindEntity.ANY_BRANCH_INDEX);

            alarmId = getBoundAlarmId(context.getInstanceId(), branchIndex);
            if (CollectionUtils.isEmpty(alarmId)) {
                return Flux.empty();
            }
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
                result.setTermSpec(AlarmInfo.parseAlarmTrigger(context, contextMap));
                return AlarmTarget
                    .of(result.getTargetType())
                    .convert(alarmData)
                    .map(result::copyWith);
            });
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
                .flatMap(e -> eventBus
                    .publish(TOPIC_ALARM_CONFIG_DELETE, e)
                    .then(
                        // 同步删除告警记录
                        alarmRecordService
                            .createDelete()
                            .where(AlarmRecordEntity::getAlarmConfigId, e.getId())
                            .execute())
                    .then())
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

        /**
         * 告警触发条件
         */
        private TermSpec termSpec;

        private boolean cached;

        private List<Map<String, Object>> bindings;

        public static TermSpec parseAlarmTrigger(ExecutionContext context, Map<String, Object> contextMap) {
            TermSpec termSpec = new TermSpec();
            Map<String, Object> configuration = context.getJob().getConfiguration();
            Object termSpecs = configuration.get(AlarmConstants.ConfigKey.alarmFilterTermSpecs);
            if (termSpecs != null) {
                termSpec.setChildren(ConverterUtils.convertToList(termSpecs,o -> FastBeanCopier.copy(o, TermSpec.class)));
                return termSpec.apply(contextMap);

            }
            return termSpec;
        }


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
    }


    public static class RecordCache implements Externalizable {

        static final byte stateNormal = 0x01;
        static final byte stateAlarming = 0x02;

        byte state;
        public long alarmTime;
        public long lastAlarmTime;


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

        public RecordCache with(AlarmResult result) {

            this.lastAlarmTime = this.alarmTime == 0 ? result.getAlarmTime() : this.alarmTime;

            this.alarmTime = result.getAlarmTime();

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

}
