package org.jetlinks.community.reactorql;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * 在接口上添加此注解,开启使用sql来处理reactor数据
 *
 * @author zhouhao
 * @see ReactorQL
 * @see EnableReactorQL
 * @since 1.6
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Indexed
public @interface ReactorQLOperation {

}
