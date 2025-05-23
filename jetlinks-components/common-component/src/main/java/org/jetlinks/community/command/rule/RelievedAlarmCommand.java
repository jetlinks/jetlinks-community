/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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