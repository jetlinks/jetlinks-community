package org.jetlinks.community.command.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandMetadataResolver;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.command.rule.data.RelieveResult;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;

@Schema(title = "解除告警命令")
public class RelievedAlarmCommand extends AbstractCommand<Mono<RelieveResult>,RelievedAlarmCommand> {

    @Schema(description = "解除告警传参信息")
    public RelieveInfo getRelieveInfo() {
        return FastBeanCopier.copy(readable(),  new RelieveInfo());
    }

    public RelievedAlarmCommand setRelieveInfo(RelieveInfo relieveInfo) {
        return with(FastBeanCopier.copy(relieveInfo, writable()));
    }

    public static FunctionMetadata metadata() {
        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        metadata.setId(CommandUtils.getCommandIdByType(RelievedAlarmCommand.class));
        metadata.setName("解除告警命令");
        metadata.setInputs(CommandMetadataResolver.resolveInputs(ResolvableType.forClass(RelieveInfo.class)));
        metadata.setOutput(CommandMetadataResolver.resolveOutput(ResolvableType.forClass(RelievedAlarmCommand.class)));
        return metadata;
    }
}