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
