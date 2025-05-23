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
package org.jetlinks.community.rule.engine.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 告警历史记录服务
 *
 * @author bestfeng
 * @author zhouhao
 * @since 2.0
 */
public interface AlarmHistoryService {

    /**
     * 分页查询告警记录
     *
     * @param queryParam 查询参数
     * @return 分页结果
     */
    Mono<PagerResult<AlarmHistoryInfo>> queryPager(QueryParam queryParam);

    /**
     * 聚合查询
     *
     * @param param 查询参数
     * @return 聚合结果
     */
    Flux<AggregationData> aggregation(AggregationQueryParam param);

    /**
     * 查询告警记录数量
     *
     * @param queryParam 查询参数
     * @return 数量
     */
    Mono<Long> count(QueryParam queryParam);

    /**
     * 不分页查询告警记录
     *
     * @param param 查询参数
     * @return 告警记录
     */
    Flux<AlarmHistoryInfo> query(QueryParam param);

    /**
     * 保存告警记录
     *
     * @param historyInfo 告警记录
     * @return void
     */
    Mono<Void> save(AlarmHistoryInfo historyInfo);

    /**
     * 保存告警记录
     *
     * @param historyInfo 告警记录
     * @return void
     */
    Mono<Void> save(Flux<AlarmHistoryInfo> historyInfo);

    /**
     * 保存告警记录
     *
     * @param historyInfo 告警记录
     * @return void
     */
    Mono<Void> save(Mono<AlarmHistoryInfo> historyInfo);
}
