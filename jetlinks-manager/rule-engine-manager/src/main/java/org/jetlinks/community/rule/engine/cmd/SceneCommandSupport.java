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
package org.jetlinks.community.rule.engine.cmd;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.community.command.CrudCommandSupport;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.scene.SceneRule;
import org.jetlinks.community.rule.engine.scene.SceneUtils;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.service.SceneService;
import org.jetlinks.sdk.server.commons.cmd.AddCommand;
import org.jetlinks.sdk.server.commons.cmd.DisabledCommand;
import org.jetlinks.sdk.server.commons.cmd.EnabledCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryByIdCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

/**
 * @author liusq
 * @date 2024/4/11
 */
public class SceneCommandSupport extends CrudCommandSupport<SceneEntity> {
    public SceneCommandSupport(SceneService service) {
        super(service);
        //新增
        registerHandler(
            AddCommand
                .<SceneRule>createHandler(
                    metadata -> metadata
                        .setInputs(Collections.singletonList(
                            SimplePropertyMetadata.of("data", "数据列表", new ArrayType().elementType(createEntityType()))
                        )),
                    cmd -> Flux
                        .fromIterable(cmd.dataList((data) -> FastBeanCopier.copy(data, new SceneRule())))
                        .flatMap(rule -> service.createScene(rule).thenReturn(rule)))
        );

        // 修改场景联动
        registerHandler(
            UpdateSceneByIdCommand
                .createHandler(cmd -> {
                    Object data = cmd.readable().getOrDefault("data", new HashMap<>());
                    return service.updateScene(cmd.getId(), FastBeanCopier.copy(data, new SceneRule())).then();
                })
        );


        // 启用
        registerHandler(
            EnabledCommand
                .createHandler(cmd -> Flux
                    .fromIterable(cmd.getIds())
                    .flatMap(service::enable)
                    .then())
        );

        // 禁用
        registerHandler(
            DisabledCommand
                .createHandler(cmd -> Flux
                    .fromIterable(cmd.getIds())
                    .flatMap(service::disabled)
                    .then())
        );

        // 根据触发器解析出支持的条件列
        registerHandler(
            ParseTermColumnCommand
                .createHandler(cmd -> {
                    SceneRule rule = FastBeanCopier.copy(cmd
                                                             .readable()
                                                             .getOrDefault("data", new HashMap<>()), new SceneRule());
                    return SceneUtils.parseTermColumns(rule);
                })
        );

        // 解析规则中输出的变量
        registerHandler(
            ParseVariablesCommand
                .createHandler(cmd -> {
                    SceneRule rule = FastBeanCopier.copy(cmd
                                                             .readable()
                                                             .getOrDefault("data", new HashMap<>()), new SceneRule());

                    Integer branch = (Integer) cmd.readable().getOrDefault("branch", 0);
                    Integer branchGroup = (Integer) cmd.readable().getOrDefault("branchGroup", 0);
                    Integer action = (Integer) cmd.readable().getOrDefault("action", 0);

                    return SceneUtils.parseVariables(Mono.just(rule), branch, branchGroup, action);
                })
        );
    }

    public static class UpdateSceneByIdCommand extends QueryByIdCommand<Mono<Void>> {
        public static CommandHandler<UpdateSceneByIdCommand, Mono<Void>> createHandler(Function<UpdateSceneByIdCommand, Mono<Void>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(UpdateSceneByIdCommand.class));
                    metadata.setName("更新场景");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                UpdateSceneByIdCommand::new
            );
        }
    }

    public static class ParseTermColumnCommand extends AbstractCommand<Flux<TermColumn>, ParseTermColumnCommand> {
        public static CommandHandler<ParseTermColumnCommand, Flux<TermColumn>> createHandler(Function<ParseTermColumnCommand, Flux<TermColumn>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(ParseTermColumnCommand.class));
                    metadata.setName("根据触发器解析出支持的条件列");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                ParseTermColumnCommand::new
            );
        }
    }

    public static class ParseVariablesCommand extends AbstractCommand<Flux<Variable>, ParseVariablesCommand> {
        public static CommandHandler<ParseVariablesCommand, Flux<Variable>> createHandler(Function<ParseVariablesCommand, Flux<Variable>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(ParseTermColumnCommand.class));
                    metadata.setName("根据触发器解析出支持的条件列");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                ParseVariablesCommand::new
            );
        }
    }
}
