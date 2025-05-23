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
package org.jetlinks.community.logging.access;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.timeseries.*;
import org.springframework.beans.factory.SmartInitializingSingleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.of;

@Slf4j
@AllArgsConstructor
public class TimeSeriesAccessLoggerService implements AccessLoggerService, SmartInitializingSingleton {

    public static final TimeSeriesMetric metric = TimeSeriesMetric.of("access_logger");

    private final TimeSeriesManager timeSeriesManager;


    @Override
    public Mono<Void> save(SerializableAccessLog log) {
        Map<String, Object> data = FastBeanCopier.copy(log, new HashMap<>());

        return timeSeriesManager
            .getService(metric)
            .commit(TimeSeriesData.of(log.getRequestTime(), data));
    }

    @Override
    public Mono<PagerResult<SerializableAccessLog>> query(QueryParamEntity queryParam) {
        return timeSeriesManager
            .getService(metric)
            .queryPager(queryParam, ts -> FastBeanCopier.copy(ts.getData(), new SerializableAccessLog()));
    }

    @Override
    public Flux<SerializableAccessLog> queryNoPaging(QueryParamEntity queryParam) {
        return timeSeriesManager
            .getService(metric)
            .query(queryParam)
            .map(ts -> FastBeanCopier.copy(ts.getData(), new SerializableAccessLog()));
    }

    @Override
    public void afterSingletonsInstantiated() {
        timeSeriesManager
            .registerMetadata(
                TimeSeriesMetadata.of(
                    metric,
                    of("requestTime", "请求时间", DateTimeType.GLOBAL),
                    of("responseTime", "响应时间", DateTimeType.GLOBAL),
                    of("target", "请求类", StringType.GLOBAL),
                    of("method", "请求方法", StringType.GLOBAL),
                    of("parameters", "参数", new ObjectType()),
                    of("action", "操作", StringType.GLOBAL),

                    of("ip", "IP地址", StringType.GLOBAL),
                    of("ipRegion", "IP属地", StringType.GLOBAL),
                    of("url", "请求地址", StringType.GLOBAL),
                    of("httpMethod", "HTTP方法", StringType.GLOBAL),
                    of("httpHeaders", "请求头", new ObjectType()),

                    of("exception", "异常信息", new StringType().expand(ConfigMetadataConstants.maxLength, 5120L)),

                    of("bindings", "绑定信息", new ArrayType().elementType(StringType.GLOBAL)),
                    of("creatorId", "创建人", StringType.GLOBAL),
                    of("spanId", "链路跨度ID", StringType.GLOBAL),
                    of("traceId", "链路ID", StringType.GLOBAL),
                    of("context", "上下文", new ObjectType()
                        .addProperty("userId", "用户ID", StringType.GLOBAL)
                        .addProperty("username", "用户名", StringType.GLOBAL)
                    )
                )
            ).subscribe(ignore -> {
                        },
                        error -> log.warn("register access logger metadata error", error));
    }
}
