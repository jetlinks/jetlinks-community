package org.jetlinks.community.reactorql;

import java.lang.annotation.*;

/**
 * 在接口的方法上注解,使用sql语句来处理参数
 *
 * @author zhouhao
 * @see org.jetlinks.reactor.ql.ReactorQL
 * @see ReactorQLOperation
 * @since 1.6
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ReactorQL {

    /**
     * 使用SQL语句来处理{@link reactor.core.publisher.Flux}操作.例如分组聚合.
     * <a href="https://doc.jetlinks.cn/dev-guide/reactor-ql.html">查看文档说明</a>
     *
     * <pre>
     *  select count(1) total,name from "arg0" group by name
     * </pre>
     * <p>
     * <p>
     * 当方法有参数时,可通过arg{index}来获取参数,如:
     * <pre>
     *     select name newName from "arg0" where id = :arg1
     * </pre>
     *
     * @return SQL语句
     */
    String[] value();

}
