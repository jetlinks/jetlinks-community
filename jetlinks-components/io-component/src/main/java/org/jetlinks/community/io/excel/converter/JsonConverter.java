package org.jetlinks.community.io.excel.converter;

import lombok.AllArgsConstructor;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.ObjectMappers;


@AllArgsConstructor
public class JsonConverter implements ConverterExcelOption {

    private final boolean array;
    private final Class<?> elementType;

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        return val == null ? null : ObjectMappers.toJsonString(val);
    }

    @Override
    public Object convertForRead(Object cell, ExcelHeader header) {
        if (array) {
            return ObjectMappers.parseJsonArray(((String) cell), elementType);
        }
        if (cell == null) {
            return null;
        }
        if (elementType.isInstance(cell)) {
            return cell;
        }
        if (cell instanceof String) {
            String json = (String) cell;
            char first = json.charAt(0);
            //只转换json格式
            if (first == '[' || first == '{') {
                return ObjectMappers.parseJsonArray(json, elementType);
            }
        }
        return FastBeanCopier
            .DEFAULT_CONVERT
            .convert(cell, elementType, FastBeanCopier.EMPTY_CLASS_ARRAY);
    }
}
