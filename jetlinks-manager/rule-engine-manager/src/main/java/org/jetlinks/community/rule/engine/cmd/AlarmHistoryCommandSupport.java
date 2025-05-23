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

import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;

/**
 * @author liusq
 * @date 2024/4/12
 */
public class AlarmHistoryCommandSupport extends AbstractCommandSupport {
    public AlarmHistoryCommandSupport(AlarmHistoryService service) {
        //分页查询
        registerHandler(
            QueryPagerCommand
                .<AlarmHistoryInfo>createHandler(
                    metadata -> {
                    },
                    cmd -> service.queryPager(cmd.asQueryParam()))
        );
    }
}
