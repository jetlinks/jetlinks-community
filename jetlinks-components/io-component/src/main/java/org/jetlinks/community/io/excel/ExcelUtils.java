package org.jetlinks.community.io.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBufAllocator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.utils.PropertiesUtils;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.reactor.excel.ExcelOption;
import org.hswebframework.reactor.excel.ReactorExcel;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.io.excel.converter.ArrayConverter;
import org.jetlinks.community.io.excel.converter.ConverterExcelOption;
import org.jetlinks.community.io.excel.converter.DateConverter;
import org.jetlinks.community.io.excel.converter.EnumConverter;
import org.jetlinks.community.io.excel.converter.StringConverter;
import org.jetlinks.core.metadata.Jsonable;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.function.Function3;

import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @since 2.1
 */
public class ExcelUtils {

    public static <T> Flux<DataBuffer> write(Class<T> header,
                                             Flux<T> dataStream,
                                             String format,
                                             ExcelOption... opts) {
        return write(getHeadersForWrite(header), dataStream, format, opts);
    }

    public static <T> Flux<DataBuffer> write(List<ExcelHeader> headers,
                                             Flux<T> dataStream,
                                             String format,
                                             ExcelOption... opts) {
        return ReactorExcel
            .writeFor(format)
            .justWrite()
            .sheet(sheet -> {
                Map<String, ExcelHeader> headerMapping = Maps.newHashMapWithExpectedSize(headers.size());
                //header 映射
                sheet.headers(headers
                    .stream()
                    .filter(head -> headerMapping.put(head.getKey(), head) == null)
                    .collect(Collectors.toList()));
                //数据
                sheet.rows(
                    dataStream.map(data -> {
                        Map<String, Object> map = data instanceof Jsonable ? ((Jsonable) data).toJson() : FastBeanCopier.copy(data, new HashMap<>());
                        return transformValue(map, headerMapping, ConverterExcelOption::convertForWrite);
                    })
                );
            })
            .option(opts)
            .writeBytes(64 * 1024)
            .map(new NettyDataBufferFactory(ByteBufAllocator.DEFAULT)::wrap);
    }

    public static <T> Flux<T> read(Supplier<T> supplier,
                                   InputStream inputStream,
                                   String format,
                                   ExcelOption... options) {
        return read(supplier, getHeadersForRead(supplier.get().getClass()), inputStream, format, options);
    }

    public static <T> Flux<T> read(Supplier<T> supplier,
                                   List<ExcelHeader> headers,
                                   InputStream inputStream,
                                   String format,
                                   ExcelOption... options) {
        Map<String, ExcelHeader> keyAndHeader = Maps.newHashMapWithExpectedSize(headers.size());
        Map<String, String> textAndHeader = headers
            .stream()
            .peek(header -> keyAndHeader.put(header.getKey(), header))
            .collect(Collectors.toMap(ExcelHeader::getText, ExcelHeader::getKey, (a, b) -> a));

        return ReactorExcel
            .<Map<String, Object>>readFor(format, HashMap::new)
            .justReadByHeader()
            .headers(textAndHeader)
            .wrapper(ReactorExcel.mapWrapper())
            .readAndClose(inputStream, options)
            .map(map -> {
                //过滤空值后转换数据
                map = transformValue(Maps.filterValues(map, val -> !ObjectUtils.isEmpty(val)),
                                     keyAndHeader,
                                     ConverterExcelOption::convertForRead);

                T data = supplier.get();

                if (data instanceof Jsonable) {
                    ((Jsonable) data).fromJson(new JSONObject(map));
                } else {
                    FastBeanCopier.copy(map, data);
                }
                return data;
            });
    }

    static Map<String, Object> transformValue(Map<String, Object> source,
                                              Map<String, ExcelHeader> headers,
                                              Function3<ConverterExcelOption, Object, ExcelHeader, Object> converter) {
        return Maps.transformEntries(source, (key, val) -> {
            ExcelHeader header = headers.get(key);
            if (header != null) {
                List<ConverterExcelOption> options = header
                    .options()
                    .getOptions(ConverterExcelOption.class);
                for (ConverterExcelOption option : options) {
                    val = converter.apply(option, val, header);
                }
            }
            return val;
        });
    }

    public static List<ExcelHeader> getHeadersForWrite(Class<?> clazz) {
        return headersCache
            .computeIfAbsent(clazz, ExcelUtils::parseHeader0)
            .stream()
            .filter(ExtExcelHeader::forWrite)
            .collect(Collectors.toList());
    }

    public static List<ExcelHeader> getHeadersForRead(Class<?> clazz) {
        return headersCache
            .computeIfAbsent(clazz, ExcelUtils::parseHeader0)
            .stream()
            .filter(ExtExcelHeader::forRead)
            .collect(Collectors.toList());
    }

    private static CellDataType convertCellType(Class<?> clazz) {

        if (Number.class.isAssignableFrom(clazz)) {
            return CellDataType.NUMBER;
        }
        if (clazz == Date.class || clazz == LocalDate.class || clazz == LocalDateTime.class) {
            return CellDataType.DATE_TIME;
        }
        return CellDataType.STRING;
    }


    private static ConverterExcelOption createConverter(Class<?> type, org.jetlinks.community.io.excel.annotation.ExcelHeader header) {
        if (type.isEnum()) {
            return new EnumConverter((Class<? extends Enum>) type);
        }
        if (header.dataType() == CellDataType.DATE_TIME
            || Date.class.isAssignableFrom(type)
            || LocalDate.class.isAssignableFrom(type)
            || LocalDateTime.class.isAssignableFrom(type)) {
            String format = header.format();
            if (!StringUtils.hasText(format)) {
                format = "yyyy/MM/dd HH:mm:ss";
            }
            return new DateConverter(format, type);
        }

        if (type == String.class) {
            return StringConverter.INSTANCE;
        }
        // TODO: 2023/6/19 更多类型转换

        return null;
    }

    @SuppressWarnings("all")
    private static ConverterExcelOption createConverter(Field field, org.jetlinks.community.io.excel.annotation.ExcelHeader header) {

        if (field.getType().isArray()) {
            Class<?> elementType = field.getType().getComponentType();
            return new ArrayConverter(true, elementType, createConverter(elementType, header));
        }

        if (List.class.isAssignableFrom(field.getType())) {
            Class<?> elementType = ResolvableType
                .forField(field)
                .getGeneric(0)
                .toClass();

            return new ArrayConverter(false, elementType, createConverter(elementType, header));
        }

        return createConverter(field.getType(), header);
    }

    @SneakyThrows
    private static List<ExtExcelHeader> parseHeader0(Class<?> clazz) {
        List<ExtExcelHeader> headers = new ArrayList<>();
        Map<ExcelHeader, Integer> sortIndex = new LinkedHashMap<>();

        int index = 0;
        for (PropertyDescriptor descriptor : PropertiesUtils.getDescriptors(clazz)) {
            Field field = PropertiesUtils.getPropertyField(clazz, descriptor.getName()).orElse(null);
            if (field == null) {
                continue;
            }
            if (field.getAnnotation(ExcelIgnore.class) != null) {
                continue;
            }
            index++;

            org.jetlinks.community.io.excel.annotation.ExcelHeader header = field
                .getAnnotation(org.jetlinks.community.io.excel.annotation.ExcelHeader.class);

            if (header == null) {
                header = descriptor
                    .getReadMethod()
                    .getAnnotation(org.jetlinks.community.io.excel.annotation.ExcelHeader.class);
            }
            if (header == null || header.ignore()) {
                continue;
            }
            CellDataType cellType = header.dataType() == CellDataType.AUTO ? convertCellType(field.getType()) : header.dataType();

            List<ExcelOption> option = new ArrayList<>();
            if (header.converter() != ConverterExcelOption.class) {
                option.add(header.converter().getConstructor().newInstance());
            } else {
                ConverterExcelOption excelOption = createConverter(field, header);
                if (null != excelOption) {
                    option.add(excelOption);
                }
            }

            for (Class<? extends ExcelOption> aClass : header.options()) {
                option.add(aClass.getConstructor().newInstance());
            }

            String[] headerTexts = header.value();
            if (headerTexts.length == 0) {
                Schema schema = field.getAnnotation(Schema.class);
                if (schema != null) {
                    headerTexts = new String[]{schema.description()};
                }
            }

            if (headerTexts.length == 0) {
                headerTexts = new String[]{field.getName()};
            }

            for (String headerText : headerTexts) {
                ExtExcelHeader excelHeader = new ExtExcelHeader(field.getName(), headerText, cellType, header);

                excelHeader.options().merge(option);

                if (header.order() != Integer.MAX_VALUE) {
                    sortIndex.put(excelHeader, header.order());
                } else {
                    sortIndex.put(excelHeader, index++);
                }
                headers.add(excelHeader);
            }

        }
        headers.sort(Comparator.comparingInt(h -> sortIndex.getOrDefault(h, Integer.MAX_VALUE)));

        return Collections.unmodifiableList(headers);
    }

    private static final Map<Class<?>, List<ExtExcelHeader>> headersCache = new ConcurrentHashMap<>();

    static class ExtExcelHeader extends ExcelHeader {
        private final org.jetlinks.community.io.excel.annotation.ExcelHeader annotation;

        public ExtExcelHeader(String key,
                              String text,
                              CellDataType type,
                              org.jetlinks.community.io.excel.annotation.ExcelHeader annotation) {
            super(key, text, type);
            this.annotation = annotation;
        }

        public boolean forRead() {
            return !annotation.ignoreRead();
        }

        public boolean forWrite() {
            return !annotation.ignoreWrite();
        }
    }
}
