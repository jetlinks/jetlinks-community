package org.jetlinks.community.annotation.command;


import org.jetlinks.core.annotation.command.CommandHandler;
import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * 标记一个类为命令服务支持端点,用于对外提供命令支持
 * <pre>{@code
 *
 *  @CommandService("myService")
 *  public class MyCommandService{
 *
 *  }
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2.3
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Indexed
public @interface CommandService {

    /**
     * 服务标识
     *
     * @return 服务标识
     */
    String id();

    /**
     * 服务名称
     *
     * @return 服务名称
     */
    String name();

    /**
     * 服务描述
     *
     * @return 服务描述
     */
    String[] description() default {};


    /**
     * 是否根据注解扫描注册服务
     *
     * @return 是否注册服务
     */
    boolean autoRegistered() default true;

    /**
     * 命令定义,用于声明支持的命令
     *
     * @return 命令定义
     */
    CommandHandler[] commands() default {};
}
