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
package org.jetlinks.community.timescaledb.impl;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.Buffered;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.timescaledb.TimescaleDBDataWriter;
import org.jetlinks.community.utils.ErrorUtils;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class DefaultTimescaleDBDataWriter implements TimescaleDBDataWriter, CommandLineRunner {

    final BufferProperties properties;
    final PersistenceBuffer<Buffer> buffer;

    final DatabaseOperator database;

    public DefaultTimescaleDBDataWriter(DatabaseOperator database,
                                        BufferProperties properties) {
        this.database = database;
        this.properties = properties;
        this.buffer = new PersistenceBuffer<>(
            BufferSettings.create(properties),
            Buffer::new,
            this::save0)
            .name("timescale-db-things-writer");
    }

    public void init() {
        buffer.init();
    }

    public void start() {
        buffer.start();
    }

    public void stop() {
        buffer.stop();
    }

    public void shutdown() {
        buffer.dispose();
    }


    @Override
    public Mono<Void> save(String metric, Map<String, Object> data) {
        return buffer
            .writeAsync(new Buffer(metric, data));
    }

    @Override
    public Mono<Void> save(String metric, Flux<Map<String, Object>> data) {
        return data
            .buffer(200)
            .concatMap(list -> save0(metric, list))
            .then();
    }

    public Mono<Boolean> save0(Collection<Buffered<Buffer>> buffers, PersistenceBuffer.FlushContext<Buffer> context) {

        Map<String, List<Buffered<Buffer>>> grouped = buffers
            .stream()
            .collect(Collectors.groupingBy(buffed -> buffed.getData().metric, Collectors.toList()));

        return Flux
            .fromIterable(grouped.entrySet())
            .flatMap(e -> this
                         .save0(e.getKey(),
                                Collections2.transform(e.getValue(), buffered -> buffered.getData().getData()))
                         .onErrorResume(err -> {
                             context.error(err);
                             if (needRetry(err)) {
                                 for (Buffered<Buffer> buffered : e.getValue()) {
                                     if (properties.isExceededRetryCount(buffered.getRetryTimes())) {
                                         buffered.dead();
                                     } else {
                                         buffered.retry(true);
                                     }
                                 }
                             } else {
                                 log.warn("save timescaledb data error [{}]", e.getKey(), err);
                                 for (Buffered<Buffer> buffered : e.getValue()) {
                                     buffered.dead();
                                 }
                             }
                             return Mono.empty();
                         }),
                     8)
            .then(Reactors.ALWAYS_FALSE);
    }

    public Mono<Void> save0(String metric, Collection<Map<String, Object>> data) {

        return database
            .getMetadata()
            .getTableOrViewReactive(metric, false)
            .cast(RDBTableMetadata.class)
            .switchIfEmpty(Mono.error(() -> new BusinessException.NoStackTrace("metric [" + metric + "] not found")))
            .flatMap(table -> database
                .dml()
                .upsert(table)
                .values(data instanceof List ? ((List<Map<String, Object>>) data) : new ArrayList<>(data))
                .execute()
                .reactive()
                .then())
            .contextWrite(ctx -> ctx.put(Logger.class, log));
    }

    private boolean needRetry(Throwable err) {
        // 数据库语法错误?
        if (ErrorUtils.hasException(err, R2dbcBadGrammarException.class)) {
            return false;
        }
        return ErrorUtils.hasException(
            err,
            R2dbcException.class,
            IOException.class,
            IllegalStateException.class,
            RejectedExecutionException.class,
            TimeoutException.class,
            DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class,
            QueryTimeoutException.class);
    }


    @Override
    public void run(String... args) {
        start();
        SpringApplication
            .getShutdownHandlers()
            .add(buffer::dispose);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Buffer implements Externalizable {
        private String metric;
        private Map<String, Object> data;

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            SerializeUtils.writeNullableUTF(metric, out);
            SerializeUtils.writeKeyValue(data, out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            metric = SerializeUtils.readNullableUTF(in);
            data = SerializeUtils.readMap(in, Maps::newHashMapWithExpectedSize);
        }
    }
}
