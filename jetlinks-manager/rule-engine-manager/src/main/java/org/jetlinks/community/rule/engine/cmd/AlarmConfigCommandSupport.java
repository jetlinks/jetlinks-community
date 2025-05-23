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

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.command.CrudCommandSupport;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.AlarmConfigDetail;
import org.jetlinks.community.rule.engine.entity.AlarmConfigEntity;
import org.jetlinks.community.rule.engine.entity.AlarmLevelEntity;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmLevelService;
import org.jetlinks.sdk.server.commons.cmd.DisabledCommand;
import org.jetlinks.sdk.server.commons.cmd.EnabledCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author liusq
 * @date 2024/4/12
 */
public class AlarmConfigCommandSupport extends CrudCommandSupport<AlarmConfigEntity> {
    public AlarmConfigCommandSupport(AlarmConfigService service,
                                     ReactiveRepository<AlarmLevelEntity, String> alarmLevelRepository) {
        super(service);
        //分页查询
        registerHandler(
            org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand
                .<AlarmConfigDetail>createHandler(
                    metadata -> {},
                    cmd -> service.queryDetailPager(cmd.asQueryParam()))
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
                    .flatMap(service::disable)
                    .then())
        );

        // 处理告警
        registerHandler(
            HandleAlarmCommand
                .createHandler(cmd -> {
                    AlarmHandleInfo info = FastBeanCopier.copy(cmd.readable(), new AlarmHandleInfo());
                    ValidatorUtils.tryValidate(info);
                    return service.handleAlarm(info);
                })
        );

        // 获取告警级别
        registerHandler(
            QueryAlarmLevelCommand
                .createHandler(cmd -> alarmLevelRepository.findById(AlarmLevelService.DEFAULT_ALARM_ID))
        );

    }

    public static class HandleAlarmCommand extends AbstractCommand<Mono<Void>, HandleAlarmCommand> {
        public static CommandHandler<HandleAlarmCommand, Mono<Void>> createHandler(Function<HandleAlarmCommand, Mono<Void>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(HandleAlarmCommand.class));
                    metadata.setName("处理告警");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                HandleAlarmCommand::new
            );
        }
    }

    public static class QueryAlarmLevelCommand extends AbstractCommand<Mono<AlarmLevelEntity>, QueryAlarmLevelCommand> {
        public static CommandHandler<QueryAlarmLevelCommand, Mono<AlarmLevelEntity>> createHandler(Function<QueryAlarmLevelCommand, Mono<AlarmLevelEntity>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(QueryAlarmLevelCommand.class));
                    metadata.setName("获取告警级别");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                QueryAlarmLevelCommand::new
            );
        }
    }
}
