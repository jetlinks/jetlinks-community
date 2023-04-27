package org.jetlinks.community.tdengine.metadata;

import org.hswebframework.ezorm.rdb.metadata.DataType;
import org.hswebframework.ezorm.rdb.metadata.dialect.DefaultDialect;

import java.math.BigDecimal;
import java.sql.JDBCType;

public class TDengineDialect extends DefaultDialect {

    public TDengineDialect() {
        super();
        registerDataType("decimal", DataType.builder(DataType.jdbc(JDBCType.DECIMAL, BigDecimal.class),
            column -> "DOUBLE"));

        registerDataType("numeric", DataType.builder(DataType.jdbc(JDBCType.NUMERIC, BigDecimal.class),
            column -> "DOUBLE"));

        registerDataType("number", DataType.builder(DataType.jdbc(JDBCType.DOUBLE, BigDecimal.class),
            column -> "DOUBLE"));

        addDataTypeBuilder(JDBCType.VARCHAR,column->"varchar("+column.getLength()+")");
        addDataTypeBuilder(JDBCType.TIMESTAMP,column->"timestamp");


    }

    @Override
    public String getQuoteStart() {
        return "`";
    }

    @Override
    public String getQuoteEnd() {
        return "`";
    }

    @Override
    public boolean isColumnToUpperCase() {
        return false;
    }

    @Override
    public String getId() {
        return "tdengine";
    }

    @Override
    public String getName() {
        return "TDengine";
    }
}
