package org.jetlinks.community.command.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandMetadataResolver;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;


@Schema(title = "触发告警命令")
public class TriggerAlarmCommand extends AbstractCommand<Mono<AlarmResult>,TriggerAlarmCommand> {
    private static final long serialVersionUID = 7056867872399432831L;


    @Schema(description = "告警传参信息")
    public AlarmInfo getAlarmInfo() {
        return FastBeanCopier.copy(readable(), new AlarmInfo());
    }

    public TriggerAlarmCommand setAlarmInfo(AlarmInfo alarmInfo) {
        return with(FastBeanCopier.copy(alarmInfo, writable()));
    }

    public static FunctionMetadata metadata() {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        metadata.setId(CommandUtils.getCommandIdByType(TriggerAlarmCommand.class));
        metadata.setName("触发告警命令");
        metadata.setInputs(CommandMetadataResolver.resolveInputs(ResolvableType.forClass(AlarmInfo.class)));
        metadata.setOutput(CommandMetadataResolver.resolveOutput(ResolvableType.forClass(TriggerAlarmCommand.class)));
        return metadata;
    }
}