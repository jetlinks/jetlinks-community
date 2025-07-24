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
package org.jetlinks.community.io.excel.annotation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelOption;
import org.jetlinks.community.io.excel.converter.ConverterExcelOption;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * 在实体类中字段或者getter方法上添加此注解,以声明表头信息
 *
 * @author zhouhao
 * @see org.jetlinks.community.io.excel.ExcelUtils#write(Class, Flux, String, ExcelOption...)
 * @see org.jetlinks.community.io.excel.ExcelUtils#read(Supplier, InputStream, String, ExcelOption...)
 * @see org.jetlinks.community.io.excel.ExcelUtils#getHeadersForWrite(Class)
 * @see org.jetlinks.community.io.excel.ExcelUtils#getHeadersForRead(Class)
 * @see org.jetlinks.community.io.excel.AbstractImporter
 * @since 2.1
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelHeader {

    /**
     * @return excel表头
     * @see Schema#description()
     * @see Field#getName()
     */
    String[] value() default {};

    /**
     * @return 表头国际化code
     */
    String i18nCode() default "";

    /**
     * @return 忽略导入导出
     */
    boolean ignore() default false;

    /**
     * @return 仅忽略导出
     */
    boolean ignoreWrite() default false;

    /**
     * @return 仅忽略导入
     */
    boolean ignoreRead() default false;

    /**
     * @return 导出时的顺序
     */
    int order() default Integer.MAX_VALUE;

    /**
     * @return 单元格数据类型
     */
    CellDataType dataType() default CellDataType.AUTO;

    /**
     * @return 单元格格式
     */
    String format() default "";

    /**
     * @return 自定义数据转换器
     */
    Class<? extends ConverterExcelOption> converter() default ConverterExcelOption.class;

    /**
     * @return 自定义其他选型配置
     */
    Class<? extends ExcelOption>[] options() default {};

    /**
     * excel表头样式
     *
     * @return 表头样式
     * @since 2.2
     */
    Style headerStyle() default @Style;

    /**
     * excel数据样式
     *
     * @return 数据样式
     * @since 2.2
     */
    Style dataStyle() default @Style;

    /**
     * 全局样式,表头和数据都会应用
     *
     * @return 2.2
     */
    Style style() default @Style;

    @Target({ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface Style {

        /**
         * 列宽度 1-255
         *
         * @return 列宽度
         */
        short width() default -1;

        /**
         * 高度
         *
         * @return 高度
         */
        short height() default -1;

        /**
         * 字体颜色
         *
         * @return 字体颜色
         */
        IndexedColors fontColor() default IndexedColors.AUTOMATIC;

        /**
         * 字体大小
         *
         * @return 字体大小
         */
        byte fontSize() default -1;

        /**
         * 边框样式: {上,右,下,左}
         *
         * @return 边框样式
         */
        BorderStyle[] border() default {};

        /**
         * 水平对齐方法
         *
         * @return 对齐方法
         */
        HorizontalAlignment align() default HorizontalAlignment.GENERAL;

        /**
         * 垂直对齐方式
         *
         * @return 对齐方式
         */
        VerticalAlignment verticalAlign() default VerticalAlignment.BOTTOM;

    }

}
