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

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.community.command.CrudCommandSupport;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author liusq
 * @date 2024/4/12
 */
public class AlarmRecordCommandSupport extends CrudCommandSupport<AlarmRecordEntity> {
    public AlarmRecordCommandSupport(AlarmRecordService service,
                                     AlarmHandleHistoryService historyService) {
        super(service);
        // 查询告警处理历史
        registerHandler(
            QueryHandleHistoryPagerCommand
                .createHandler(cmd -> historyService.queryPager(cmd.asQueryParam()))
        );
    }

    public static class QueryHandleHistoryPagerCommand extends QueryPagerCommand<AlarmHandleHistoryEntity> {
        public static CommandHandler<QueryHandleHistoryPagerCommand, Mono<PagerResult<AlarmHandleHistoryEntity>>> createHandler(Function<QueryHandleHistoryPagerCommand, Mono<PagerResult<AlarmHandleHistoryEntity>>> handler) {
            return CommandHandler.of(
                () -> {
                    SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
                    metadata.setId(CommandUtils.getCommandIdByType(QueryHandleHistoryPagerCommand.class));
                    metadata.setName("获取告警处理历史");
                    return metadata;
                },
                (cmd, ignore) -> handler.apply(cmd),
                QueryHandleHistoryPagerCommand::new
            );
        }
    }
}
