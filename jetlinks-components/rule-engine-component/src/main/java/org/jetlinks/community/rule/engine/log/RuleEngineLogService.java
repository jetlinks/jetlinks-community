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
package org.jetlinks.community.rule.engine.log;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteEventInfo;
import org.jetlinks.community.rule.engine.entity.RuleEngineExecuteLogInfo;
import reactor.core.publisher.Mono;

/**
 * 规则引擎日志服务,用于查询规则执行日志信息
 *
 * @since 1.8
 */
public interface RuleEngineLogService {

    /**
     * 分页查询规则执行事件日志
     *
     * @param queryParam 查询参数
     * @return 分页查询结果
     */
    Mono<PagerResult<RuleEngineExecuteEventInfo>> queryEvent(QueryParam queryParam);

    /**
     * 分页查询规则日志
     *
     * @param queryParam 查询参数
     * @return 分页查询结果
     */
    Mono<PagerResult<RuleEngineExecuteLogInfo>> queryLog(QueryParam queryParam);

}
