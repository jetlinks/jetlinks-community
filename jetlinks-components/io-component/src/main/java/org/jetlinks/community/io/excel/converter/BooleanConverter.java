package org.jetlinks.community.io.excel.converter;

import org.hswebframework.reactor.excel.ExcelHeader;
import org.jetlinks.reactor.ql.utils.CastUtils;

public class BooleanConverter implements ConverterExcelOption {
    public static final BooleanConverter INSTANCE = new BooleanConverter();
    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        if (val == null) {
            return null;
        }
        return val instanceof Boolean ? ((Boolean) val) ? "是" : "否" : String.valueOf(val);
    }

    @Override
    public Object convertForRead(Object val, ExcelHeader header) {
        if (val == null) {
            return null;
        }
        return CastUtils.castBoolean(val);
    }
}
