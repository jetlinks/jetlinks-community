package org.jetlinks.community.form.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.ValueCodec;
import org.jetlinks.core.metadata.DataType;

import java.sql.JDBCType;

@Getter
@AllArgsConstructor(staticName = "of")
public class SimpleFieldType implements FieldType {
    private final String id;
    private final Class<?> javaType;
    private final JDBCType jdbcType;
    private final ValueCodec<?, ?> codec;
    private final int length;
    private final int scale;
    private final DataType dataType;


}
