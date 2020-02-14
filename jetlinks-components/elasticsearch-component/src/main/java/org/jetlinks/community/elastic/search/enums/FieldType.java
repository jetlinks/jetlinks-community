package org.jetlinks.community.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.exception.NotFoundException;
import org.springframework.util.StringUtils;

@AllArgsConstructor
@Getter
public enum FieldType implements EnumDict<String> {

    TEXT("text", "text"),
    BYTE("byte", "byte"),
    SHORT("short", "short"),
    INTEGER("int", "integer"),
    LONG("long", "long"),
    DATE("date", "date"),
    HALF_FLOAT("half_float", "half_float"),
    FLOAT("float", "float"),
    DOUBLE("double", "double"),
    BOOLEAN("boolean", "boolean"),
    OBJECT("object", "object"),
    AUTO("auto", "auto"),
    NESTED("nested", "nested"),
    IP("ip", "ip"),
    ATTACHMENT("attachment", "attachment"),
    KEYWORD("string", "keyword");

    @Getter
    private String text;

    @Getter
    private String value;

    public static FieldType of(Object value) {
        if (!StringUtils.isEmpty(value)) {
            for (FieldType fieldType : FieldType.values()) {
                if (fieldType.getValue().equals(value)) {
                    return fieldType;
                }
            }
        }
        throw new NotFoundException("未找到数据类型为：" + value + "的枚举");
    }

    public static FieldType ofJava(Object value) {
        if (!StringUtils.isEmpty(value)) {
            for (FieldType fieldType : FieldType.values()) {
                if (fieldType.getText().equals(value)) {
                    return fieldType;
                }
            }
        }
        throw new NotFoundException("未找到数据类型为：" + value + "的枚举");
    }
}
