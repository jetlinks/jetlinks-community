package org.jetlinks.community.io.excel.converter;

import lombok.AllArgsConstructor;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.ConverterUtils;

import java.util.List;

/**
 * @since 2.1
 */
@AllArgsConstructor
public class ArrayConverter implements ConverterExcelOption{

    private boolean array;

    private Class<?> elementType;

    private ConverterExcelOption converter;


    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        return String.join(",",
            ConverterUtils.convertToList(val, v -> {
                if (converter == null) {
                    return String.valueOf(v);
                }
                return String.valueOf(converter.convertForWrite(v, header));
            }));
    }

    @Override
    public Object convertForRead(Object cell, ExcelHeader header) {

        List<Object> list = ConverterUtils
            .convertToList(cell, val -> {
                if (converter != null) {
                    val = converter.convertForRead(val, header);
                }
                if (elementType.isInstance(val)) {
                    return val;
                }
                return FastBeanCopier.DEFAULT_CONVERT
                    .convert(val, elementType, FastBeanCopier.EMPTY_CLASS_ARRAY);
            });

        if (array) {
            return list.toArray();
        }

        return list;
    }
}
