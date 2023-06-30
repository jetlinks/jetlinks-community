package org.jetlinks.community.io.excel.annotation;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelOption;
import org.jetlinks.community.io.excel.converter.ConverterExcelOption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Target({ElementType.FIELD,ElementType.METHOD})
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

}
