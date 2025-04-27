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
        schema.addFeature(new TimescaleDBCreateTableSqlBuilder());
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
