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
package org.jetlinks.community.iotdb;

import org.jetlinks.community.timeseries.TimeSeriesData;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IoTDBOperations extends Disposable {

    String getDatabase();

    Mono<Void> execute(String sql);

    Flux<Map<String, Object>> query(String sql);

    Mono<Void> ensureTable(String metric, List<SchemaColumn> columns);

    Mono<Void> write(String metric, Flux<TimeSeriesData> data, Set<String> tagColumns);
}
