package org.jetlinks.community.form.type;

import org.hswebframework.ezorm.core.ValueCodec;
import org.jetlinks.core.metadata.DataType;

import java.sql.JDBCType;


public interface FieldType {

    String getId();

    Class<?> getJavaType();

    JDBCType getJdbcType();

    ValueCodec<?, ?> getCodec();

    default int getLength() {
        return 255;
    }

    default int getScale() {
        return 2;
    }

    DataType getDataType();
}
