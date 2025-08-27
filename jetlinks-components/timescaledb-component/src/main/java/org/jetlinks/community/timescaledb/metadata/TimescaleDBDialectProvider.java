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
package org.jetlinks.community.timescaledb.metadata;

import org.hswebframework.ezorm.rdb.codec.DateTimeCodec;
import org.hswebframework.ezorm.rdb.metadata.DefaultValueCodecFactory;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.dialect.Dialect;
import org.hswebframework.ezorm.rdb.supports.postgres.PostgresqlSchemaMetadata;
import org.hswebframework.web.crud.configuration.DialectProvider;

import java.sql.JDBCType;
import java.util.Date;

public class TimescaleDBDialectProvider implements DialectProvider {

    public static final TimescaleDBDialectProvider GLOBAL = new TimescaleDBDialectProvider();

    public static final String NAME = "timescaledb";

    @Override
    public String name() {
        return "timescaledb";
    }

    @Override
    public Dialect getDialect() {
        return Dialect.POSTGRES;
    }

    @Override
    public String getBindSymbol() {
        return "$";
    }

    @Override
    public RDBSchemaMetadata createSchema(String name) {
        PostgresqlSchemaMetadata schema = new PostgresqlSchemaMetadata(name);
        schema.addFeature(new TimescaleDBCreateTableSqlBuilder(name));
        schema.addFeature(new TimescaleDBAlterTableSqlBuilder());
        DefaultValueCodecFactory codecFactory = new DefaultValueCodecFactory();
        codecFactory
            .register(col -> "jsonb".equals(col.getDataType()) ||
                          "json".equals(col.getDataType()),
                      col -> new JsonbValueCodec(true));

        codecFactory
            .register(col -> col.getType().getSqlType() == JDBCType.TIMESTAMP
                || col.getType().getSqlType() == JDBCType.TIMESTAMP_WITH_TIMEZONE,
                      col -> new DateTimeCodec("yyyy-MM-dd HH:mm:ss.SSS", Date.class));

        schema.addFeature(codecFactory);
        return schema;
    }
}
