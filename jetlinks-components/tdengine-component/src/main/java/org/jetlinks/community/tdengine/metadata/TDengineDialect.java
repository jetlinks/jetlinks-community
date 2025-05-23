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
