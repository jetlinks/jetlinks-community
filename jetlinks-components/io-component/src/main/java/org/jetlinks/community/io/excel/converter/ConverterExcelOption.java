package org.jetlinks.community.io.excel.converter;

import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.reactor.excel.ExcelOption;

/**
 * @since 2.1
 */
public interface ConverterExcelOption extends ExcelOption {

    Object convertForWrite(Object val, ExcelHeader header);

    Object convertForRead(Object cell, ExcelHeader header);

}
