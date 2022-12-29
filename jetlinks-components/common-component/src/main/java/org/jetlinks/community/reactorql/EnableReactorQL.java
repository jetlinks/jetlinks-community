package org.jetlinks.community.reactorql;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 在配置类上加上此注解,并指定{@link EnableReactorQL#value()},将扫描指定包下注解了{@link ReactorQLOperation}的接口类,
 * 并生成代理对象注入到spring中.
 *
 * @author zhouhao
 * @since  1.6
 * @see ReactorQL
 * @see ReactorQLOperation
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ReactorQLBeanDefinitionRegistrar.class)
public @interface EnableReactorQL {
    /**
     * @return 扫描的包名
     */
    String[] value() default {};
}
