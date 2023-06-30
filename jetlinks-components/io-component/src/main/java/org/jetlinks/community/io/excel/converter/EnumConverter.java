package org.jetlinks.community.io.excel.converter;

import lombok.AllArgsConstructor;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.i18n.LocaleUtils;

import java.util.Locale;
import java.util.Objects;

@AllArgsConstructor
public class EnumConverter implements ConverterExcelOption {

    @SuppressWarnings("all")
    private final Class<? extends Enum> type;

    @Override
    public Object convertForWrite(Object val, ExcelHeader header) {
        if (val instanceof EnumDict) {
            return ((EnumDict<?>) val).getI18nMessage(LocaleUtils.current());
        }
        if (val instanceof Enum) {
            return ((Enum<?>) val).name();
        }

        return val;
    }

    @Override
    @SuppressWarnings("all")
    public Object convertForRead(Object val, ExcelHeader header) {
        if (val == null) {
            return null;
        }
        if (EnumDict.class.isAssignableFrom(type)) {
            Locale locale = LocaleUtils.current();
            return EnumDict
                .find((Class) type, e -> {
                    return e.eq(val) || Objects.equals(val, e.getI18nMessage(locale));
                })
                .orElse(null);
        }
        return Enum.valueOf(type, String.valueOf(val));
    }
}
