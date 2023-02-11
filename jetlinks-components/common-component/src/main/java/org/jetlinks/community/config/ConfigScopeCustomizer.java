package org.jetlinks.community.config;

/**
 * 实现此接口,自定义配置域以及配置定义
 *
 * @author zhouhao
 * @since 2.0
 */
public interface ConfigScopeCustomizer {

    /**
     * 执行自定义,通过manager来添加自定义作用域
     *
     * @param manager manager
     */
    void custom(ConfigScopeManager manager);

}
