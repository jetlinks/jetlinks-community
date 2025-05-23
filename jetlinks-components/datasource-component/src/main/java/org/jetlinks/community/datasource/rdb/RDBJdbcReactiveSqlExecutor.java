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
package org.jetlinks.community.datasource.rdb;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.jdbc.JdbcReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@AllArgsConstructor
@Slf4j
public class RDBJdbcReactiveSqlExecutor extends JdbcReactiveSqlExecutor {
    private final DataSource dataSource;

    @Override
    public Mono<Connection> getConnection() {
        return Mono
            .using(dataSource::getConnection,
                   Mono::just,
                   source -> {
                       try {
                           source.close();
                       } catch (SQLException e) {
                           log.error(e.getMessage(), e);
                       }
                   },
                   false
            );
    }

    @Override
    public Mono<Void> execute(Publisher<SqlRequest> request) {
        return super
            .execute(request)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Integer> update(Publisher<SqlRequest> request) {
        return super
            .update(request)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public <E> Flux<E> select(Publisher<SqlRequest> request, ResultWrapper<E, ?> wrapper) {
        return super
            .select(request, wrapper)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
