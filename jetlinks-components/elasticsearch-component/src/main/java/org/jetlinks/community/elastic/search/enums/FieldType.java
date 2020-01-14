package org.jetlinks.community.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.exception.NotFoundException;
import org.springframework.util.StringUtils;

@AllArgsConstructor

public enum FieldType implements EnumDict<String> {

    TEXT("text"),
    BYTE("byte"),
    SHORT("short"),
    INTEGER("integer"),
    LONG("long"),
    DATE("date"),
    HALF_FLOAT("half_float"),
    FLOAT("float"),
    DOUBLE("double"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    AUTO("auto"),
    NESTED("nested"),
    IP("ip"),
    ATTACHMENT("attachment"),
    KEYWORD("keyword");

    @Override
    public String getText() {
        return value;
    }

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
}
