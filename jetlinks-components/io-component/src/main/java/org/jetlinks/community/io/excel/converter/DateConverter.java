package org.jetlinks.community.io.excel.converter;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.reactor.excel.WritableCell;
import org.hswebframework.reactor.excel.poi.options.CellOption;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@AllArgsConstructor
public class DateConverter implements ConverterExcelOption, CellOption {

    private final String format;

    private final Class<?> javaType;

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        return new DateTime(CastUtils.castDate(val)).toString(format);
    }

    @Override
    public Object convertForRead(Object val, ExcelHeader header) {

        if (null == val) {
            return null;
        }

        if (javaType.isInstance(val)) {
            return val;
        }
        Date date = CastUtils.castDate(val);
        if (javaType == Long.class || javaType == long.class) {
            return date.getTime();
        }
        if (javaType == LocalDateTime.class) {
            return java.time.LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (javaType == LocalDate.class) {
            return java.time.LocalDateTime
                .ofInstant(date.toInstant(), ZoneId.systemDefault())
                .toLocalDate();
        }

        return date;
    }

    @Override
    public void cell(org.apache.poi.ss.usermodel.Cell poiCell, WritableCell cell) {
        CellStyle style = poiCell.getCellStyle();
        if (style == null) {
            style = poiCell.getRow()
                           .getSheet()
                           .getWorkbook()
                           .createCellStyle();
            poiCell.setCellStyle(style);
        }
        DataFormat dataFormat = poiCell
            .getRow()
            .getSheet()
            .getWorkbook()
            .createDataFormat();

        style.setDataFormat(dataFormat.getFormat(format));

    }
}
