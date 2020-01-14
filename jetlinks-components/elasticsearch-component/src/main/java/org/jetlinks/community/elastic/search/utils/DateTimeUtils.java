package org.jetlinks.community.elastic.search.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class DateTimeUtils {

    public static Object formatDateToTimestamp(Object date, List<String> formats) {
        return TermCommonUtils.getStandardsTermValue(
                formatDateArrayToTimestamp(TermCommonUtils.convertToList(date), formats)
        );
    }

    private static Object formatDateStringToTimestamp(String dateString, List<String> formats) {
        for (String format : formats) {
            try {
                return formatDateStringToTimestamp(dateString, format);
            } catch (Exception e) {
                log.debug("按格式:{}解析时间字符串:{}错误", format, dateString);
            }
        }
        throw new UnsupportedOperationException("不支持的时间转换" + "formats:" +
                JSON.toJSONString(formats) + "dateString:" + dateString);
    }

    private static long formatDateStringToTimestamp(String dateString, String format) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime dateTime = LocalDateTime.parse(dateString, dateTimeFormatter);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static List<Object> formatDateArrayToTimestamp(List<Object> values, List<String> formats) {
        List<Object> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String) {
                result.add(formatDateStringToTimestamp(value.toString(), formats));
            } else {
                result.add(value);
            }
        }
        return result;
    }
}
