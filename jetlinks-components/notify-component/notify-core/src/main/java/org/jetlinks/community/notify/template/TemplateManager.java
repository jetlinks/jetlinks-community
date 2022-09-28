package org.jetlinks.community.notify.template;

import org.jetlinks.community.notify.NotifyType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 模版管理器,用于统一管理通知模版. 模版的转换由不同的通知服务商{@link TemplateProvider}去实现.
 *
 * @author zhouhao
 * @see Template
 * @see TemplateProvider
 * @since 1.0
 */
public interface TemplateManager {

    /**
     * 根据通知类型和模版ID获取模版
     * <p>
     * 如果模版不存在将返回{@link Mono#empty()},可通过{@link Mono#switchIfEmpty(Mono)}进行处理.
     * <p>
     * 如果通知类型或者通知服务商不支持，将会返回{@link Mono#error(Throwable)}. {@link UnsupportedOperationException}
     * <p>
     * 请根据不同的通知类型处理对应的模版.
     *
     * @param type 通知类型
     * @param id   模版ID
     * @return 模版
     */
    @Nonnull
    Mono<? extends Template> getTemplate(@Nonnull NotifyType type, @Nonnull String id);

    /**
     * 根据通知类型和配置对象创建一个模版
     * <p>
     * 如果通知类型或者通知服务商不支持，将会返回{@link Mono#error(Throwable)}. {@link UnsupportedOperationException}
     * <p>
     * 请根据不同的通知类型处理对应的模版.
     *
     * @param type       通知类型
     * @param properties 模版配置
     * @return 模版
     * @see TemplateProvider
     */
    @Nonnull
    Mono<? extends Template> createTemplate(@Nonnull NotifyType type, @Nonnull TemplateProperties properties);

    /**
     * 重新加载模版
     *
     * @param templateId 模版ID
     * @return 异步结果
     */
    @Nonnull
    Mono<Void> reload(String templateId);
}
