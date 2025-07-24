/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.io.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBufAllocator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.hswebframework.ezorm.rdb.utils.PropertiesUtils;
import org.hswebframework.reactor.excel.*;
import org.hswebframework.reactor.excel.poi.options.CellOption;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.io.excel.converter.*;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.core.metadata.Jsonable;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.function.Function3;
import reactor.util.function.Tuples;

import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @since 2.1
 */
public class ExcelUtils {

    /**
     * 导出数据流为指定格式的文件流
     *
     * @param header     表头实体类,使用实体类来定义表头.
     * @param dataStream 数据流
     * @param format     文件格式: xlsx,csv
     * @param opts       自定义操作
     * @param <T>        数据类型
     * @return 文件流
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     * @see org.hswebframework.reactor.excel.poi.options.SheetOption
     * @see org.hswebframework.reactor.excel.poi.options.RowOption
     */
    public static <T> Flux<DataBuffer> write(Class<T> header,
                                             Flux<T> dataStream,
                                             String format,
                                             ExcelOption... opts) {
        List<ExcelHeader> headers = getHeadersForWrite(header);
        //样式操作
        ExcelOption[] optCopy = Arrays.copyOf(opts, opts.length + 1);
        optCopy[optCopy.length - 1] = new ExcelStyleOption(
            header.getAnnotation(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style.class),
            headers);
        return write(headers, dataStream, format, optCopy);
    }

    /**
     * 导出数据流为指定格式的文件流
     *
     * @param headers    表头.
     * @param dataStream 数据流
     * @param format     文件格式: xlsx,csv
     * @param opts       自定义操作
     * @param <T>        数据类型
     * @return 文件流
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     * @see org.hswebframework.reactor.excel.poi.options.SheetOption
     * @see org.hswebframework.reactor.excel.poi.options.RowOption
     * @see ExcelUtils#getHeadersForWrite(Class)
     */
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

                        Map<String, Object> map = data instanceof Jsonable
                            ? ((Jsonable) data).toJson()
                            : FastBeanCopier.copy(data, new HashMap<>());

                        return transformValue(map, headerMapping, ConverterExcelOption::convertForWrite);
                    })
                );
            })
            .option(opts)
            .writeBytes(64 * 1024)
            .map(new NettyDataBufferFactory(ByteBufAllocator.DEFAULT)::wrap);
    }

    /**
     * 基于实体类中定义的表头信息，读取文件流为实体数据。
     *
     * @param supplier    实体类构造器
     * @param inputStream 文件流
     * @param format      文件格式: xlsx,csv
     * @param options     自定义操作
     * @param <T>         实体类型
     * @return 实体数据流
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     */
    public static <T> Flux<T> read(Supplier<T> supplier,
                                   InputStream inputStream,
                                   String format,
                                   ExcelOption... options) {
        return read(supplier, getHeadersForRead(supplier.get().getClass()), inputStream, format, options);
    }

    /**
     * 指定表头信息读取文件流为实体数据
     *
     * @param supplier    实体类构造器
     * @param headers     表头信息
     * @param inputStream 文件流
     * @param format      文件格式: xlsx,csv
     * @param options     自定义操作
     * @param <T>         实体类型
     * @return 实体数据流
     */
    public static <T> Flux<T> read(Supplier<T> supplier,
                                   List<ExcelHeader> headers,
                                   InputStream inputStream,
                                   String format,
                                   ExcelOption... options) {
        return read0((ignore, source, map) -> {
            T data = supplier.get();
            if (data instanceof Jsonable) {
                ((Jsonable) data).fromJson(new JSONObject(map));
            } else {
                FastBeanCopier.copy(map, data);
            }
            return data;
        }, headers, inputStream, format, options);
    }

    static <T> Flux<T> read0(Function3<Long, Map<String, Object>, Map<String, Object>, T> converter,
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
            .index((idx, map) -> {
                //过滤空值后转换数据
                return converter.apply(idx,
                                       map,
                                       transformValue(Maps.filterValues(map, val -> !ObjectUtils.isEmpty(val)),
                                                      keyAndHeader,
                                                      ConverterExcelOption::convertForRead));
            });
    }

    /**
     * 转换读取后的结果,用于值类型转换操作,如: 将数据字典的text转换为value.
     *
     * @param source    源数据
     * @param headers   表头
     * @param converter 转换逻辑
     * @return 转换后的结果
     */
    static Map<String, Object> transformValue(Map<String, Object> source,
                                              Map<String, ExcelHeader> headers,
                                              Function3<ConverterExcelOption, Object, ExcelHeader, Object> converter) {
        return Maps.transformEntries(source, (key, val) -> {
            ExcelHeader header = headers.get(key);
            if (header != null) {
                List<ConverterExcelOption> options = header
                    .options()
                    .getOptions(ConverterExcelOption.class);
                //类型转换
                for (ConverterExcelOption option : options) {
                    val = converter.apply(option, val, header);
                }
            }
            return val;
        });
    }

    /**
     * 获取用于写(导出)的表头信息
     *
     * @param clazz 导出类
     * @return 表头信息
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     */
    public static List<ExcelHeader> getHeadersForWrite(Class<?> clazz) {
        return headersCache
            .computeIfAbsent(clazz, ExcelUtils::parseHeader0)
            .stream()
            .filter(ExtExcelHeader::forWrite)
            .collect(Collectors.toList());
    }

    /**
     * 获取用于读(导入)的表头信息
     *
     * @param clazz 导出类
     * @return 表头信息
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     */
    public static List<ExcelHeader> getHeadersForRead(Class<?> clazz) {
        return headersCache
            .computeIfAbsent(clazz, ExcelUtils::parseHeader0)
            .stream()
            .filter(ExtExcelHeader::forRead)
            .collect(Collectors.toList());
    }

    /**
     * 根据java类型获取单元格数据类型
     *
     * @param clazz java类型
     * @return 单元格数据类型
     */
    public static CellDataType convertCellType(Class<?> clazz) {

        if (Number.class.isAssignableFrom(clazz)) {
            return CellDataType.NUMBER;
        }
        if (clazz == Date.class || clazz == LocalDate.class || clazz == LocalDateTime.class) {
            return CellDataType.DATE_TIME;
        }
        return CellDataType.STRING;
    }

    /**
     * 根据实体定义的字段信息获取值转换操作
     *
     * @param field  字段
     * @param type   类型
     * @param header 表头注解
     * @return 转换逻辑
     */
    private static ConverterExcelOption createConverter(Field field, Class<?> type, org.jetlinks.community.io.excel.annotation.ExcelHeader header) {
        if (type.isEnum() || Enum.class.isAssignableFrom(type)) {
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

        org.jetlinks.community.dictionary.Dictionary dictionary = field.getAnnotation( org.jetlinks.community.dictionary.Dictionary.class);
        if (dictionary != null) {
            return new DictionaryConverter(dictionary.value(), type);
        }
        //基础类型
        if (type.isPrimitive()) {
            return null;
        }
        if (Map.class.isAssignableFrom(type) || Serializable.class.isAssignableFrom(type)) {
            return new JsonConverter(false, type);
        }
        // TODO: 2023/6/19 更多类型转换

        return null;
    }

    /**
     * 根据实体定义的字段信息获取值转换操作
     *
     * @param field  字段
     * @param header 表头注解
     * @return 转换逻辑
     */
    @SuppressWarnings("all")
    private static ConverterExcelOption createConverter(Field field, org.jetlinks.community.io.excel.annotation.ExcelHeader header) {

        if (field.getType().isArray()) {
            Class<?> elementType = field.getType().getComponentType();
            ConverterExcelOption converter = createConverter(field, elementType, header);
            if (converter instanceof JsonConverter) {
                return new JsonConverter(true, elementType);
            }
            return new ArrayConverter(true, elementType, converter);
        }

        if (List.class.isAssignableFrom(field.getType())) {
            Class<?> elementType = ResolvableType
                .forField(field)
                .getGeneric(0)
                .toClass();
            ConverterExcelOption converter = createConverter(field, elementType, header);
            if (converter instanceof JsonConverter) {
                return new JsonConverter(true, elementType);
            }
            return new ArrayConverter(false, elementType, createConverter(field, elementType, header));
        }

        return createConverter(field, field.getType(), header);
    }

    /**
     * 解析java类中定义的表头信息
     *
     * @param clazz java类
     * @return 表头信息
     * @see org.jetlinks.community.io.excel.annotation.ExcelHeader
     * @see ExcelIgnore
     */
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
            //忽略导入导出
            if (field.getAnnotation(ExcelIgnore.class) != null ||
                descriptor.getReadMethod().getAnnotation(ExcelIgnore.class) != null) {
                continue;
            }
            index++;

            //优先getter方法
            org.jetlinks.community.io.excel.annotation.ExcelHeader header = descriptor
                .getReadMethod()
                .getAnnotation(org.jetlinks.community.io.excel.annotation.ExcelHeader.class);
            //字段上的注解
            if (header == null) {
                header = field
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
            //没有指定表头,尝试使用swagger来配置.
            if (headerTexts.length == 0) {
                Schema schema = field.getAnnotation(Schema.class);
                if (schema != null) {
                    headerTexts = new String[]{schema.description()};
                }
            }
            //没有表头使用字段名
            if (headerTexts.length == 0) {
                headerTexts = new String[]{field.getName()};
            }
            //可以支持定义多个header映射一个字段
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
        private final String i18nCode;

        public ExtExcelHeader(String key,
                              String text,
                              CellDataType type,
                              org.jetlinks.community.io.excel.annotation.ExcelHeader annotation) {
            super(key, text, type);
            this.annotation = annotation;
            this.i18nCode = annotation.i18nCode();
        }

        @Override
        public String getText() {
            if (StringUtils.hasText(i18nCode)) {
                return LocaleUtils.resolveMessage(i18nCode, super.getText());
            }
            return super.getText();
        }

        public boolean forRead() {
            return !annotation.ignoreRead();
        }

        public boolean forWrite() {
            return !annotation.ignoreWrite();
        }
    }

    @AllArgsConstructor
    @org.jetlinks.community.io.excel.annotation.ExcelHeader.Style
    static class ExcelStyleOption implements CellOption {
        static final Map<String, Object> DEFAULT_STYLE = AnnotationUtils
            .getAnnotationAttributes(ExcelStyleOption.class.getAnnotation(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style.class));

        private final org.jetlinks.community.io.excel.annotation.ExcelHeader.Style defaultStyle;
        private final List<ExcelHeader> headers;

        private final Map<Workbook, StyleCache> bookCache = new ConcurrentHashMap<>();

        private final Map<org.jetlinks.community.io.excel.annotation.ExcelHeader.Style, Boolean> customStyle = new ConcurrentHashMap<>();

        @Override
        public void cell(Cell poiCell, WritableCell cell) {

            if (poiCell.getColumnIndex() >= headers.size()) {
                return;
            }
            //全局默认样式
            if (defaultStyle != null) {
                accept(defaultStyle, poiCell, cell);
            }

            ExcelHeader header = headers.get(poiCell.getColumnIndex());

            if (header instanceof ExtExcelHeader) {

                //列默认样式
                accept(((ExtExcelHeader) header).annotation.style(), poiCell, cell);
                //单元格样式
                if (poiCell.getRowIndex() == 0) {
                    accept(((ExtExcelHeader) header).annotation.headerStyle(), poiCell, cell);
                } else {
                    accept(((ExtExcelHeader) header).annotation.dataStyle(), poiCell, cell);
                }
            }
        }

        void applySize(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style style, Cell poiCell) {
            if (style.width() > 0 && poiCell.getRowIndex() <= 1) {
                poiCell
                    .getRow()
                    .getSheet()
                    .setColumnWidth(poiCell.getColumnIndex(), Math.min(255, style.width()) * 256);
            }

            if (style.height() > 0) {
                //全局样式只修改一次高度
                if (style != defaultStyle || poiCell.getColumnIndex() == 0) {
                    poiCell
                        .getRow()
                        .setHeight((short) (style.height() * 20));
                }
            }

        }

        void accept(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style style, Cell poiCell, WritableCell cell) {
            applySize(style, poiCell);

            if (customStyle(style)) {
                Workbook workbook = poiCell.getRow().getSheet().getWorkbook();
                StyleCache cache = getStyleCache(workbook);
                CellStyle oldStyle = poiCell.getCellStyle();
                CellStyle cellStyle = cache
                    .getOrCreateStyle(Tuples.of(style, poiCell.getColumnIndex()), (_s, s) -> {
                        copyStyle(workbook, oldStyle, s);
                        applyStyle(_s.getT1(), workbook, s);
                    });
                poiCell.setCellStyle(cellStyle);
            }
        }

        void copyStyle(Workbook workbook, CellStyle source, CellStyle target) {
            if (source == null) {
                return;
            }
            target.cloneStyleFrom(source);
        }

        private boolean customFont(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style style) {
            return style.fontSize() > 0 || style.fontColor() != IndexedColors.AUTOMATIC;
        }

        private boolean customStyle(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style style) {
            return customStyle.computeIfAbsent(style, _style -> !Objects.equals(ObjectMappers.toJsonString(DEFAULT_STYLE),
                                                                                ObjectMappers.toJsonString(AnnotationUtils.getAnnotationAttributes(_style))));
        }

        private void copyFont(Font source, Font target) {
            if (source == null || target == null) {
                return;
            }
            target.setColor(source.getColor());
            target.setFontHeightInPoints(source.getFontHeightInPoints());
            target.setFontName(source.getFontName());
        }

        private void applyStyle(org.jetlinks.community.io.excel.annotation.ExcelHeader.Style style,
                                Workbook workbook,
                                CellStyle cellStyle) {
            if (style == null) {
                return;
            }
            //字体
            if (customFont(style)) {
                Font font = workbook.createFont();
                Font oldFont = workbook.getFontAt(cellStyle.getFontIndex());
                copyFont(oldFont, font);
                font.setColor(style.fontColor().getIndex());
                if (style.fontSize() != -1) {
                    font.setFontHeightInPoints(style.fontSize());
                }
                cellStyle.setFont(font);
            }

            //对齐方式
            if (style.align() != HorizontalAlignment.GENERAL) {
                cellStyle.setAlignment(style.align());
            }
            if (style.verticalAlign() != VerticalAlignment.BOTTOM) {
                cellStyle.setVerticalAlignment(style.verticalAlign());
            }

            //边框
            BorderStyle[] border = style.border();
            if (border.length > 0) {
                cellStyle.setBorderTop(border[0]);
            }
            if (border.length > 1) {
                cellStyle.setBorderRight(border[1]);
            }
            if (border.length > 2) {
                cellStyle.setBorderBottom(border[2]);
            }
            if (border.length > 3) {
                cellStyle.setBorderLeft(border[3]);
            }
        }

        private StyleCache getStyleCache(Workbook workbook) {
            return bookCache.computeIfAbsent(workbook, StyleCache::new);
        }

        @AllArgsConstructor
        static class StyleCache {
            final Workbook workbook;
            final Map<Object, CellStyle> fontCache = new HashMap<>();

            <T> CellStyle getOrCreateStyle(T key, BiConsumer<T, CellStyle> factory) {
                return fontCache.computeIfAbsent(key, k -> {
                    CellStyle style = workbook.createCellStyle();
                    factory.accept(key, style);
                    return style;
                });
            }
        }

    }

}
