package org.jetlinks.community.spi;

import reactor.core.Disposable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 通用提供商管理接口,用于提供通用的提供商支持
 *
 * @param <T> 类型
 * @author zhouhao
 * @since 2.2
 */
public interface Provider<T> {
    /**
     * 根据类型创建Provider
     *
     * @param type 类型
     * @param <T>  类型
     * @return Provider
     */
    static <T> Provider<T> create(Class<? super T> type) {
        return new SimpleProvider<>(type.getSimpleName());
    }

    /**
     * 注册一个提供商,可通过调用返回值{@link Disposable#dispose()}来注销.
     *
     * @param id       ID
     * @param provider 提供商实例
     * @return Disposable
     */
    Disposable register(String id, T provider);

    /**
     * 获取所有的注册的提供商
     *
     * @return 提供商
     */
    List<T> getAll();

    /**
     * 注册提供商,如果已经存在则忽略注册,并返回旧的提供商实例
     *
     * @param id       ID
     * @param provider 提供商实例
     * @return 旧的提供商实例
     */
    T registerIfAbsent(String id, T provider);

    /**
     * 注册提供商,如果已经存在则忽略注册,并返回旧的提供商实例,否则返回注册后的提供商实例
     *
     * @param id              ID
     * @param providerBuilder 提供商构造器
     * @return 如果已经存在则忽略注册, 并返回旧的提供商实例, 否则返回注册后的提供商实例
     */
    T registerIfAbsent(String id, Function<String, T> providerBuilder);

    /**
     * 根据ID注销提供商
     *
     * @param id ID
     */
    void unregister(String id);

    /**
     * 根据ID和具体的实例注销提供商
     *
     * @param id       ID
     * @param provider 提供商实例
     */
    void unregister(String id, T provider);

    /**
     * 注册监听钩子,当提供商注册和注销时调用对应方法进行业务逻辑处理.
     * 可通过调用返回值{@link Disposable#dispose()}来注销钩子
     *
     * @param hook Hook
     * @return Disposable
     */
    Disposable addHook(Hook<T> hook);

    /**
     * 根据ID获取提供商
     *
     * @param id ID
     * @return Optional
     */
    Optional<T> get(String id);

    /**
     * 根据ID获取提供商,如果不存在将抛出{@link UnsupportedOperationException}
     *
     * @param id ID
     * @return 提供商
     */
    T getNow(String id);

    interface Hook<T> {
        /**
         * 当注册时调用
         *
         * @param provider 提供商实例
         */
        void onRegister(T provider);

        /**
         * 当注销时调用
         *
         * @param provider 提供商实例
         */
        void onUnRegister(T provider);
    }
}
