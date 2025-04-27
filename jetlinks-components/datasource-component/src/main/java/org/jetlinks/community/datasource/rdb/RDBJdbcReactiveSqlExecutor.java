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
