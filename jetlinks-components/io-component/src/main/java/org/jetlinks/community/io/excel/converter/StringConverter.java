package org.jetlinks.community.io.excel.converter;

import org.hswebframework.reactor.excel.ExcelHeader;

public class StringConverter implements ConverterExcelOption {
    public static final StringConverter INSTANCE = new StringConverter();

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        return val == null ? null : String.valueOf(val);
    }

    @Override
    public Object convertForRead(Object cell, ExcelHeader header) {

        return cell == null ? null : String.valueOf(cell);
    }
}
