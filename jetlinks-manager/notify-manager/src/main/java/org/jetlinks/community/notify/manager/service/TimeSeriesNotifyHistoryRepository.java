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
package org.jetlinks.community.notify.manager.service;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.*;

@AllArgsConstructor
public class TimeSeriesNotifyHistoryRepository implements NotifyHistoryRepository, SmartInitializingSingleton {

    public static final TimeSeriesMetric METRIC = TimeSeriesMetric.of("notify_history");

    private final TimeSeriesManager timeSeriesManager;

    @Subscribe("/notify/**")
    @Transactional(propagation = Propagation.NEVER)
    public Mono<Void> handleNotify(SerializableNotifierEvent event) {
        return timeSeriesManager
            .getService(METRIC)
            .commit(TimeSeriesData.of(event.getSendTime(), NotifyHistory.of(event).toJson()));
    }


    @Override
    public Mono<PagerResult<NotifyHistory>> queryPager(QueryParamEntity param) {
        return timeSeriesManager
            .getService(METRIC)
            .queryPager(param, map -> {
                NotifyHistory history = FastBeanCopier.copy(map.getData(), new NotifyHistory(), "context");
                history.setContext(JSON.parseObject((String) map.get("context").orElse("{}")));
                return history;
            });
    }

    @Override
    public void afterSingletonsInstantiated() {

        timeSeriesManager
            .registerMetadata(
                TimeSeriesMetadata.of(
                    METRIC,
                    of("id", "ID", StringType.GLOBAL),
                    of("provider", "通知提供商", StringType.GLOBAL),
                    of("notifyType", "通知类型", StringType.GLOBAL),
                    of("notifierId", "通知器ID", StringType.GLOBAL),
                    of("state", "通知状态", StringType.GLOBAL),
                    of("notifyTime", "通知时间", DateTimeType.GLOBAL),
                    of("context", "通知上下文", new StringType()
                        .expand(ConfigMetadataConstants.maxLength, 8096L)),
                    of("template", "模版内容", new StringType()
                        .expand(ConfigMetadataConstants.maxLength, 8096L)),
                    of("errorType", "错误类型", StringType.GLOBAL),
                    of("errorStack", "异常栈", new StringType()
                        .expand(ConfigMetadataConstants.maxLength, 8096L)),
                    of("templateId", "模版ID", StringType.GLOBAL)
                )
            )
            .block(Duration.ofSeconds(10));
    }
}
