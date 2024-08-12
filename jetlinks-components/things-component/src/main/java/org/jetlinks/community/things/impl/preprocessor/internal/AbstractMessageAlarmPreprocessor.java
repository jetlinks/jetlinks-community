package org.jetlinks.community.things.impl.preprocessor.internal;

import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.command.CommandSupportManagerProviders;
import org.jetlinks.community.command.InternalSdkServices;
import org.jetlinks.community.command.rule.RuleCommandServices;
import org.jetlinks.community.command.rule.TriggerAlarmCommand;
import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.core.Value;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.PropertyMetadata;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 告警预处理器实现,用于消息不符合预期规则时触发告警
 * <p>
 * 通过实现{@link ThingMessagePreprocessorMatcher}来自定义预期匹配逻辑.
 *
 * @author bestfeng
 */
public abstract class AbstractMessageAlarmPreprocessor extends MatcherThingMessagePreprocessor {

    private final int alarmLevel;

    private final String alarmName;

    private final String preprocessId;

    private final String alarmConfigSource;

    protected abstract String getThingNameKey();

    private final Set<String> configKeys = new HashSet<>();

    public static final String ALARM_NAME = "alarmName";

    public static final String ALARM_LEVEL = "alarmLevel";


    public AbstractMessageAlarmPreprocessor(ThingMessagePreprocessorMatcher matcher,
                                            Config config) {
        super(matcher);
        this.alarmLevel = config.getInt(ALARM_LEVEL, 3);
        this.alarmName = config.getString(ALARM_NAME, null);
        this.alarmConfigSource = buildAlarmConfigSource(config);
        this.preprocessId = config.getPreprocessId();
        configKeys.add(getThingNameKey());
    }

    @Override
    protected Mono<ThingMessage> process(ThingMessagePreprocessorMatcher.Result result, Context context) {

        String thingType = context.getThing().getType().getId();
        String thingTypeName = context.getThing().getType().getName();
        String thingId = context.getThing().getId();
        return Mono
            .zip(
                context.getThing().getSelfConfigs(configKeys),
                context.getThing()
                       .getMetadata()
                       .mapNotNull(metadata -> metadata
                           .getPropertyOrNull(context
                                                  .unwrap(PropertiesContext.class)
                                                  .getProperty()
                                                  .getId()))

            )
            .flatMap(t2 -> {
                String thingName = t2.getT1().getValue(getThingNameKey()).map(Value::asString).orElse(null);
                AlarmInfo alarmInfo = new AlarmInfo();
                alarmInfo.setLevel(alarmLevel);
                alarmInfo.setTargetType(thingType);
                alarmInfo.setTargetName(thingName);
                alarmInfo.setTargetId(thingId);
                alarmInfo.setSourceType(thingType);
                alarmInfo.setSourceName(thingName);
                alarmInfo.setSourceId(thingId);
                alarmInfo.setAlarmName(alarmName == null ?
                                           createDefaultAlarmName(thingTypeName, thingName, t2.getT2()) : alarmName);
                alarmInfo.setAlarmConfigId(preprocessId);
                alarmInfo.setAlarmConfigSource(alarmConfigSource);
                alarmInfo.setTermSpec(TermSpec.ofTermSpecs(result.getSpec()));
                alarmInfo.setData(context.getMessage().toJson());
                TriggerAlarmCommand triggerAlarmCommand = new TriggerAlarmCommand();
                return executeCommandToMono(
                    triggerAlarmCommand.setAlarmInfo(alarmInfo))
                    .thenReturn(context.getMessage());
            });
    }

    protected String buildAlarmConfigSource(Config config) {
        //device-property-preprocessor
        StringJoiner joiner = new StringJoiner("-", "", "");
        joiner.add(config.getThingType());
        joiner.add(config.getMetadataId().getType().name());
        joiner.add("preprocessor");
        return joiner.toString();
    }

    protected String createDefaultAlarmName(String thingTypeName, String thingName, PropertyMetadata propertyMetadata) {
        return LocaleUtils.resolveMessage(
            "message.property_threshold_alarm_name",
            "属性阈值告警",
            thingTypeName,
            thingName,
            propertyMetadata.getName()
        );
    }

    private Mono<Void> executeCommandToMono(Command<?> command) {
        return Mono
            .justOrEmpty(CommandSupportManagerProviders.getProvider(InternalSdkServices.ruleService))
            .flatMap(provider -> provider.getCommandSupport(RuleCommandServices.alarm, Collections.emptyMap()))
            .flatMap(commandSupport -> commandSupport.executeToMono(command))
            .then();
    }
}
