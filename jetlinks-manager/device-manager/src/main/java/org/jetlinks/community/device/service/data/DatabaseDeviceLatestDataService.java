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
package org.jetlinks.community.device.service.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrappers;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.record.Record;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.ezorm.rdb.operator.ddl.TableBuilder;
import org.hswebframework.ezorm.rdb.operator.dml.SelectColumnSupplier;
import org.hswebframework.ezorm.rdb.operator.dml.query.Selects;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.device.entity.DeviceLatestData;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.jetlinks.community.timeseries.query.AggregationColumn;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.math.MathFlux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.JDBCType;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备最新数据服务,用于保存设备最新的相关数据到关系型数据库中，可以使用动态条件进行查询相关数据
 *
 * @author zhouhao
 * @since 1.5.0
 */
@Slf4j
public class DatabaseDeviceLatestDataService implements DeviceLatestDataService, CommandLineRunner {

    private final DatabaseOperator databaseOperator;

    private final BufferProperties buffer;

    private PersistenceBuffer<Buffer> writer;

    public DatabaseDeviceLatestDataService(DatabaseOperator databaseOperator, BufferProperties properties) {
        this.databaseOperator = databaseOperator;
        this.buffer = properties;
        init();
    }

    public static String getLatestTableTableName(String productId) {
        return StringBuilderUtils.buildString(productId, (p, b) -> {
            b.append("dev_lst_");
            for (char c : productId.toCharArray()) {
                if (c == '-' || c == '.') {
                    b.append('_');
                } else {
                    b.append(Character.toLowerCase(c));
                }
            }
        });
    }

    private String getEventColumn(String event, String property) {
        return event + "_" + property;
    }

    private Mono<Boolean> doWrite(Flux<Buffer> flux) {
        return flux
            .groupBy(Buffer::getTable, Integer.MAX_VALUE)
            .concatMap(group -> group
                .groupBy(Buffer::getDeviceId, Integer.MAX_VALUE)
                .flatMap(sameDevice -> sameDevice.reduce(Buffer::merge))
                .buffer(200)
                //批量更新
                .flatMap(sameTableData -> {
                    Buffer first = sameTableData.get(0);
                    List<Map<String, Object>> data = Lists.transform(sameTableData, Buffer::getProperties);
                    return this
                        .doUpdateLatestData(first.table, data)
                        .onErrorResume((err) -> {
                            log.error("save device latest data error", err);
                            return Mono.empty();
                        });
                }, 8, 8)
            )
            .then(Reactors.ALWAYS_FALSE);

    }

    public void init() {

        writer = new PersistenceBuffer<>(
                BufferSettings.create("./data/buffer", buffer),
                Buffer::new,
                this::doWrite)
            .name("device-latest-data");

        writer.init();

    }

    public void destroy() {
        writer.stop();
    }

    public Mono<Void> reloadMetadata(String productId, DeviceMetadata metadata) {
        return Mono
            .defer(() -> {
                String tableName = getLatestTableTableName(productId);
                log.debug("reload product[{}] metadata,table name:[{}] ", productId, tableName);
                RDBSchemaMetadata schema = databaseOperator.getMetadata()
                                                           .getCurrentSchema();

                RDBTableMetadata table = schema.newTable(tableName);

                RDBColumnMetadata id = table.newColumn();
                id.setName("id");
                id.setLength(64);
                id.setPrimaryKey(true);
                id.setJdbcType(JDBCType.VARCHAR, String.class);
                table.addColumn(id);

                RDBColumnMetadata deviceName = table.newColumn();
                deviceName.setLength(128);
                deviceName.setName("device_name");
                deviceName.setAlias("deviceName");
                deviceName.setJdbcType(JDBCType.VARCHAR, String.class);
                table.addColumn(deviceName);

                for (PropertyMetadata property : metadata.getProperties()) {
                    table.addColumn(ThingsDatabaseUtils.convertColumn(property));
                }
                for (EventMetadata event : metadata.getEvents()) {
                    DataType type = event.getType();
                    if (type instanceof ObjectType) {
                        for (PropertyMetadata property : ((ObjectType) type).getProperties()) {
                            RDBColumnMetadata column = ThingsDatabaseUtils.convertColumn(property);
                            column.setName(getEventColumn(event.getId(), property.getId()));
                            table.addColumn(column);
                        }
                    }
                }

                return schema
                    .getTableReactive(tableName, false)
                    .doOnNext(oldTable -> oldTable.replace(table))
                    .switchIfEmpty(Mono.fromRunnable(() -> schema.addTable(table)))
                    .then();
            });
    }

    public Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata, boolean ddl) {
        return Mono
            .defer(() -> {
                String tableName = getLatestTableTableName(productId);
                log.debug("upgrade product[{}] metadata,table name:[{}] ", productId, tableName);
                TableBuilder builder = databaseOperator
                    .ddl()
                    .createOrAlter(tableName)
                    .addColumn("id").primaryKey().varchar(64).commit()
                    .addColumn("device_name").alias("deviceName").varchar(128).notNull().commit()
                    .merge(true)
                    .allowAlter(ddl);

                for (PropertyMetadata property : metadata.getProperties()) {
                    builder.addColumn(ThingsDatabaseUtils.convertColumn(property));
                }
                for (EventMetadata event : metadata.getEvents()) {
                    DataType type = event.getType();
                    if (type instanceof ObjectType) {
                        for (PropertyMetadata property : ((ObjectType) type).getProperties()) {
                            RDBColumnMetadata column = ThingsDatabaseUtils.convertColumn(property);
                            column.setName(getEventColumn(event.getId(), property.getId()));
                            builder.addColumn(column);
                        }
                    }
                }
                return builder
                    .commit()
                    .reactive()
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
            });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> upgradeMetadata(String productId, DeviceMetadata metadata) {
        return upgradeMetadata(productId, metadata, true);
    }

    @Override
    @Subscribe(topics = "/device/**", features = Subscription.Feature.local)
    public Mono<Void> saveAsync(DeviceMessage message) {
        try {
            Map<String, Object> properties = DeviceMessageUtils
                .tryGetProperties(message)
                .orElseGet(() -> {
                    //事件
                    if (message instanceof EventMessage) {
                        Object data = ((EventMessage) message).getData();
                        String event = ((EventMessage) message).getEvent();
                        if (data instanceof Map) {
                            Map<?, ?> mapValue = (Map<?, ?>) data;
                            Map<String, Object> val = Maps.newHashMapWithExpectedSize(mapValue.size());
                            ((Map<?, ?>) data).forEach((k, v) -> val.put(getEventColumn(event, String.valueOf(k)), v));
                            return val;
                        }
                        return Collections.singletonMap(getEventColumn(event, "value"), data);
                    }
                    return null;
                });
            if (CollectionUtils.isEmpty(properties)) {
                return Mono.empty();
            }
            String productId = message.getHeader("productId").map(String::valueOf).orElse("null");
            String deviceName = message.getHeader("deviceName").map(String::valueOf).orElse(message.getDeviceId());
            String tableName = getLatestTableTableName(productId);
            Map<String, Object> prob = new HashMap<>(properties);
            prob.put("id", message.getDeviceId());
            prob.put("deviceName", deviceName);

            Buffer buffer = Buffer.of(tableName, message.getDeviceId(), deviceName, prob, message.getTimestamp());
            return writer.writeAsync(buffer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Mono.empty();
    }

    public void save(DeviceMessage message) {
        saveAsync(message).subscribe();
    }

    @Override
    public void run(String... args) throws Exception {
        writer.start();
        SpringApplication
            .getShutdownHandlers()
            .add(writer::dispose);
    }

    @Getter
    private static class Buffer implements Externalizable {
        //有效期
        private final static long expires = Duration.ofSeconds(30).toMillis();

        private String table;

        private String deviceId;

        private String deviceName;

        private Map<String, Object> properties;

        private long timestamp;


        public Buffer() {
        }

        public boolean isEffective() {
            return System.currentTimeMillis() - timestamp < expires;
        }

        public static Buffer of(String table,
                                String deviceId,
                                String deviceName,
                                Map<String, Object> properties,
                                long timestamp) {
            Buffer buffer = new Buffer();
            buffer.table = table;
            buffer.deviceId = deviceId;
            buffer.deviceName = deviceName;
            buffer.properties = properties;
            buffer.timestamp = timestamp;
            return buffer;
        }

        public Buffer merge(Buffer buffer) {

            //以比较新的数据为准
            if (buffer.timestamp > this.timestamp) {
                return buffer.merge(this);
            }
            //合并
            buffer.properties.forEach(properties::putIfAbsent);
            return this;
        }

        int size() {
            return properties == null ? 0 : properties.size();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(table);
            out.writeUTF(deviceId);
            out.writeUTF(deviceName);
            out.writeLong(timestamp);
            SerializeUtils.writeObject(properties, out);
        }

        @Override
        @SuppressWarnings("all")
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            table = in.readUTF();
            deviceId = in.readUTF();
            deviceName = in.readUTF();
            timestamp = in.readLong();
            properties = (Map<String, Object>) SerializeUtils.readObject(in);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Mono<Void> doUpdateLatestData(String table,
                                         List<Map<String, Object>> properties) {
        return databaseOperator
            .getMetadata()
            .getCurrentSchema()
            .getTableReactive(table, false)
            .flatMap(_table -> {
                //没有deviceName,说明可能在同步表结构的时候发生了错误。
                if (!_table.getColumn("deviceName").isPresent()) {
                    log.warn("设备最新数据表[{}]结构错误", table);
                    return Mono.empty();
                }
                return databaseOperator
                    .dml()
                    .upsert(_table)
                    .ignoreUpdate("id")
                    .values(properties)
                    .execute()
                    .reactive()
                    .then();
            });
    }

    public ReactiveRepository<Record, String> getRepository(String productId) {
        return databaseOperator
            .dml()
            .createReactiveRepository(getLatestTableTableName(productId));
    }

    @Override
    public Flux<DeviceLatestData> query(String productId, QueryParamEntity param) {
        return getRepository(productId)
            .createQuery()
            .setParam(param)
            .fetch()
            .map(DeviceLatestData::new);
    }

    @Override
    public Mono<DeviceLatestData> queryDeviceData(String productId, String deviceId) {
        return getRepository(productId)
            .findById(deviceId)
            .map(DeviceLatestData::new);
    }

    @Override
    public Mono<Integer> count(String productId, QueryParamEntity param) {
        return getRepository(productId)
            .createQuery()
            .setParam(param)
            .count();
    }

    private SelectColumnSupplier createAggColumn(AggregationColumn column) {
        switch (column.getAggregation()) {
            case COUNT:
                return Selects.count(column.getProperty()).as(column.getAlias());
            case AVG:
                return Selects.avg(column.getProperty()).as(column.getAlias());
            case MAX:
                return Selects.max(column.getProperty()).as(column.getAlias());
            case MIN:
                return Selects.min(column.getProperty()).as(column.getAlias());
            case SUM:
                return Selects.sum(column.getProperty()).as(column.getAlias());
            default:
                throw new UnsupportedOperationException("unsupported agg:" + column.getAggregation());
        }
    }

    private SelectColumnSupplier[] createAggColumns(List<AggregationColumn> columns) {
        return columns
            .stream()
            .map(this::createAggColumn)
            .toArray(SelectColumnSupplier[]::new);
    }

    @Override
    public Mono<Map<String, Object>> aggregation(String productId,
                                                 List<AggregationColumn> columns,
                                                 QueryParamEntity paramEntity) {
        if (CollectionUtils.isEmpty(columns)) {
            return Mono.error(new ValidationException("columns", "error.aggregate_column_cannot_be_empty"));
        }
        String table = getLatestTableTableName(productId);

        return databaseOperator
            .getMetadata()
            .getTableReactive(table)
            .flatMap(tableMetadata ->
                     {
                         List<String> illegals = new ArrayList<>();

                         List<AggregationColumn> columnList = columns
                             .stream()
                             .filter(column -> {
                                 if (tableMetadata
                                     .getColumn(column.getProperty())
                                     .isPresent()) {
                                     return true;
                                 }
                                 illegals.add(column.getProperty());
                                 return false;
                             })
                             .collect(Collectors.toList());
                         if (CollectionUtils.isEmpty(columnList)) {
                             return Mono.error(new ValidationException("columns", "error.invalid_product_attribute_or_event", productId, illegals));
                         }
                         return databaseOperator
                             .dml()
                             .query(table)
                             .select(createAggColumns(columnList))
                             .setParam(paramEntity.clone().noPaging())
                             .fetch(ResultWrappers.map())
                             .reactive()
                             .take(1)
                             .singleOrEmpty()
                             .doOnNext(map -> {
                                 for (AggregationColumn column : columns) {
                                     map.putIfAbsent(column.getAlias(), 0);
                                 }
                             })
                             //表不存在
                             .onErrorReturn(e -> StringUtils.hasText(e.getMessage()) && e
                                 .getMessage()
                                 .contains("doesn't exist "), Collections.emptyMap());
                     }
            );

    }

    @Override
    public Flux<Map<String, Object>> aggregation(Flux<QueryProductLatestDataRequest> param,
                                                 boolean merge) {
        Flux<QueryProductLatestDataRequest> cached = param.cache();
        return cached
            .flatMap(request -> this
                .aggregation(request.getProductId(), request.getColumns(), request.getQuery())
                .doOnNext(map -> {
                    if (!merge) {
                        map.put("productId", request.getProductId());
                    }
                }))
            .as(flux -> {
                if (!merge) {
                    return flux;
                }
                //合并所有产品的字段到一条数据中,合并时,使用第一个聚合字段使用的聚合类型
                return cached
                    .take(1)
                    .flatMapIterable(QueryLatestDataRequest::getColumns)
                    .collectMap(AggregationColumn::getAlias, agg -> aggMappers.getOrDefault(agg.getAggregation(), sum))
                    .flatMap(mappers -> flux
                        .flatMapIterable(Map::entrySet)
                        .groupBy(Map.Entry::getKey, Integer.MAX_VALUE)
                        .flatMap(group -> mappers
                            .getOrDefault(group.key(), sum)
                            .apply(group.map(Map.Entry::getValue))
                            .map(val -> Tuples.of(String.valueOf(group.key()), (Object) val)))
                        .collectMap(Tuple2::getT1, Tuple2::getT2)).flux();
            });
    }


    static Map<Aggregation, Function<Flux<Object>, Mono<? extends Number>>> aggMappers = new HashMap<>();

    static Function<Flux<Object>, Mono<? extends Number>> avg = flux -> MathFlux.averageDouble(flux
                                                                                                   .map(CastUtils::castNumber)
                                                                                                   .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> max = flux -> MathFlux.max(flux
                                                                                         .map(CastUtils::castNumber)
                                                                                         .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> min = flux -> MathFlux.min(flux
                                                                                         .map(CastUtils::castNumber)
                                                                                         .map(Number::doubleValue));
    static Function<Flux<Object>, Mono<? extends Number>> sum = flux -> MathFlux.sumDouble(flux
                                                                                               .map(CastUtils::castNumber)
                                                                                               .map(Number::doubleValue));

    static {
        aggMappers.put(Aggregation.AVG, avg);
        aggMappers.put(Aggregation.MAX, max);
        aggMappers.put(Aggregation.MIN, min);
        aggMappers.put(Aggregation.SUM, sum);
        aggMappers.put(Aggregation.COUNT, sum);
    }

}
