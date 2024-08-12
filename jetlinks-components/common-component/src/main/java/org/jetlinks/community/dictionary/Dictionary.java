package org.jetlinks.community.dictionary;

import org.hswebframework.ezorm.rdb.mapping.annotation.Codec;
import org.hswebframework.web.dict.EnumDict;

import java.lang.annotation.*;
import java.util.Collection;

/**
 * 定义字段是一个数据字典,和枚举的使用方式类似.
 * <p>
 * 区别是数据的值通过{@link Dictionaries}进行获取.
 *
 * <pre>{@code
 *   public class MyEntity{
 *
 *      //数据库存储的是枚举的值
 *       @Column(length=32)
 *       @Dictionary("my_status")
 *       @ColumnType(javaType=String.class)
 *       private EnumDict<String> status;
 *
 *       @Column
 *       @Dictionary("my_types")
 *       //使用long来存储数据,表示使用字段的序号来进行mask运算进行存储.
 *       @ColumnType(javaType=Long.class,jdbcType=JDBCType.BIGINT)
 *       private EnumDict<String>[] types;
 *   }
 * }</pre>
 * <b>⚠️注意</b>
 * <ul>
 *     <li>
 *         字段类型只支持{@code EnumDict<String>},{@code EnumDict<String>[]},{@code List<EnumDict<String>>}
 *     </li>
 *     <li>
 *         多选时建议使用位掩码来存储: {@code @ColumnType(javaType=Long.class,jdbcType=JDBCType.BIGINT) },便于查询.
 *     </li>
 *     <li>使用位掩码存储字典值时,基于{@link EnumDict#ordinal()}进行计算,因此字段选项数量不能超过64个,修改字典时,请注意序号值变化。</li>
 *     <li>模块需要引入依赖:<pre>{@code
 *   <dependency>
 *      <groupId>org.hswebframework.web</groupId>
 *      <artifactId>hsweb-system-dictionary</artifactId>
 *   </dependency>
 *     }</pre></li>
 * </ul>
 *
 *
 * @author zhouhao
 * @see EnumDict#getValue()
 * @see EnumDict#getMask()
 * @see Dictionaries
 * @see Dictionaries#toMask(Collection)
 * @see DictionaryManager
 * @since 2.2
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Codec
public @interface Dictionary {

    /**
     * 数据字典ID
     *
     * @return 数据字典ID
     * @see Dictionaries#getItem(String, String)
     */
    String value();

}
