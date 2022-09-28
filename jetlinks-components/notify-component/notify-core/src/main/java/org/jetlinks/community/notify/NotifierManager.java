package org.jetlinks.community.notify;

import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 通知管理器,用于获取获取通知器.
 *
 * @author zhouhao
 * @since  1.0
 * @see Notifier
 * @see DefaultNotifyType
 * @see Template
 * @see NotifyConfigManager
 */
public interface NotifierManager {

    /**
     * 获取通知器
     *
     * @param type 通知类型 {@link DefaultNotifyType}
     * @param id   唯一标识
     * @param <T>  模版类型
     * @return 异步获取结果
     * @see NotifierProvider
     */
    @Nonnull
    <T extends Template> Mono<Notifier<T>> getNotifier(@Nonnull NotifyType type, @Nonnull String id);

    @Nonnull
    <T extends Template> Mono<Notifier<T>> createNotifier(NotifierProperties properties);

    /**
     * 重新加载通知管理器
     *
     * @param id 通知管理器ID
     * @return 加载结果
     */
    Mono<Void> reload(String id);

}
