package org.jetlinks.community.form.type;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.ValueObject;

import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
public class FieldTypeSpec implements ValueObject {
    private String name;

    private int length;
    private int scale;

    private JDBCType jdbcType;

    private Map<String,Object> configuration;

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
