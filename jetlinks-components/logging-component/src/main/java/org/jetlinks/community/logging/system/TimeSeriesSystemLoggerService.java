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
package org.jetlinks.community.logging.system;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.springframework.beans.factory.SmartInitializingSingleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.of;

@Slf4j
@AllArgsConstructor
public class TimeSeriesSystemLoggerService implements SystemLoggerService, SmartInitializingSingleton {

    public static final TimeSeriesMetric metric = TimeSeriesMetric.of("system_logger");

    private final TimeSeriesManager timeSeriesManager;


    @Override
    public Mono<Void> save(SerializableSystemLog log) {
        Map<String, Object> data = FastBeanCopier.copy(log, new HashMap<>());

        return timeSeriesManager
            .getService(metric)
            .commit(TimeSeriesData.of(log.getCreateTime(), data));
    }

    @Override
    public Mono<PagerResult<SerializableSystemLog>> query(QueryParamEntity queryParam) {
        return timeSeriesManager
            .getService(metric)
            .queryPager(queryParam, ts -> FastBeanCopier.copy(ts.getData(), new SerializableSystemLog()));
    }

    @Override
    public Flux<SerializableSystemLog> queryNoPaging(QueryParamEntity queryParam) {
        return timeSeriesManager
            .getService(metric)
            .query(queryParam)
            .map(ts -> FastBeanCopier.copy(ts.getData(), new SerializableSystemLog()));
    }

    @Override
    public void afterSingletonsInstantiated() {

        timeSeriesManager
            .registerMetadata(
                TimeSeriesMetadata.of(
                    metric,
                    of("createTime", "创建时间", DateTimeType.GLOBAL),
                    of("name", "日志名", StringType.GLOBAL),
                    of("level", "日志级别", StringType.GLOBAL),
                    of("message", "日志内容", new StringType().expand(ConfigMetadataConstants.maxLength, 5120L)),
                    of("className", "类名", StringType.GLOBAL),
                    of("exceptionStack", "异常栈", new StringType().expand(ConfigMetadataConstants.maxLength, 5120L)),
                    of("methodName", "方法名", StringType.GLOBAL),
                    of("lineNumber", "行号", IntType.GLOBAL),
                    of("threadId", "线程ID", StringType.GLOBAL),
                    of("threadName", "线程名称", StringType.GLOBAL),
                    of("spanId", "链路跨度ID", StringType.GLOBAL),
                    of("traceId", "链路ID", StringType.GLOBAL),
                    of("context", "上下文", new ObjectType()
                        .addProperty("userId", "用户ID", StringType.GLOBAL)
                        .addProperty("username", "用户名", StringType.GLOBAL)
                    )
                )
            ).subscribe(ignore -> {
                        },
                        error -> log.warn("register system logger metadata error", error));
    }
}
