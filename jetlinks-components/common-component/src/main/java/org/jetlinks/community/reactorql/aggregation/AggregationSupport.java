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
package org.jetlinks.community.reactorql.aggregation;

import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.dml.FunctionColumn;
import org.jetlinks.community.spi.Provider;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 聚合函数支持.
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
public interface AggregationSupport extends Function<Publisher<?>, Mono<?>> {

    Provider<AggregationSupport> supports = Provider.create(AggregationSupport.class);

    String getId();

    String getName();

    SqlFragments createSql(FunctionColumn column);


    static AggregationSupport getNow(String id) {
        return AggregationSupport.supports
            .get(id.toUpperCase())
            .orElseGet(() -> AggregationSupport.supports
                .get(id.toLowerCase())
                .orElseGet(() -> AggregationSupport.supports.getNow(id)));
    }
}
