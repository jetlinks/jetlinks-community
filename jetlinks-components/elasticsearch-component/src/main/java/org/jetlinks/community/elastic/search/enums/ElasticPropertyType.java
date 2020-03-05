package org.jetlinks.community.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.*;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@AllArgsConstructor
public enum ElasticPropertyType implements EnumDict<String> {

    TEXT("text", "text", StringType::new),
    BYTE("byte", "byte", () -> new IntType().min(Byte.MIN_VALUE).max(Byte.MAX_VALUE)),
    SHORT("short", "short", () -> new IntType().min(Short.MIN_VALUE).max(Short.MAX_VALUE)),
    INTEGER("int", "integer", IntType::new),
    LONG("long", "long", LongType::new),
    DATE("date", "date", DateTimeType::new),
    HALF_FLOAT("half_float", "half_float", FloatType::new),
    FLOAT("float", "float", FloatType::new),
    DOUBLE("double", "double", DoubleType::new),
    BOOLEAN("boolean", "boolean", BooleanType::new),
    OBJECT("object", "object", ObjectType::new),
    AUTO("auto", "auto", () -> null),
    NESTED("nested", "nested", ObjectType::new),
    IP("ip", "ip", LongType::new),
    ATTACHMENT("attachment", "attachment", FileType::new),
    KEYWORD("string", "keyword", StringType::new),
    GEO_POINT("geo_point", "geo_point", GeoType::new);

    @Getter
    private String text;
    @Getter
    private String value;

    private Supplier<DataType> typeBuilder;

    public DataType getType() {
        return typeBuilder.get();
    }

    public static ElasticPropertyType of(Object value) {
        if (!StringUtils.isEmpty(value)) {
            for (ElasticPropertyType elasticPropertyType : ElasticPropertyType.values()) {
                if (elasticPropertyType.getValue().equals(value)) {
                    return elasticPropertyType;
                }
            }
        }
        return null;
    }

    public static ElasticPropertyType ofJava(Object value) {
        if (!StringUtils.isEmpty(value)) {
            for (ElasticPropertyType elasticPropertyType : ElasticPropertyType.values()) {
                if (elasticPropertyType.getText().equals(value)) {
                    return elasticPropertyType;
                }
            }
        }
        throw new NotFoundException("未找到数据类型为：" + value + "的枚举");
    }
}
