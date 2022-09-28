package org.jetlinks.community.notify;

import org.jetlinks.core.Values;
import org.jetlinks.core.Wrapper;
import org.jetlinks.community.notify.template.Template;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * 通知器,用于发送通知,如: 短信,邮件,语音,微信等
 *
 * @author zhouhao
 * @see NotifierManager
 * @see NotifierProvider
 * @since 1.0
 */
public interface Notifier<T extends Template> extends Wrapper {

    /**
     * @return 通知器ID
     */
    String getNotifierId();

    /**
     * 获取通知类型,如: 语音通知
     *
     * @return 通知类型
     * @see DefaultNotifyType
     */
    @Nonnull
    NotifyType getType();

    /**
     * 获取通知服务提供商,如: aliyun 等
     *
     * @return 通知服务提供商
     */
    @Nonnull
    Provider getProvider();

    /**
     * 指定模版ID进行发送.
     * 发送失败或者模版不存在将返回{@link Mono#error(Throwable)}.
     *
     * @param templateId 模版ID
     * @param context    上下文
     * @return 异步发送结果
     * @see Mono#doOnError(Consumer)
     * @see Mono#doOnSuccess(Consumer)
     * @see Template
     */
    @Nonnull
    Mono<Void> send(@Nonnull String templateId, Values context);

    /**
     * 指定模版{@link Template}并发送.
     * <p>
     * 注意:不同等服务商使用的模版实现不同.
     * <p>
     * 发送失败返回{@link Mono#error(Throwable)}.
     *
     * @param template 模版
     * @param context  上下文
     * @return 异步发送结果
     * @see Mono#doOnError(Consumer)
     * @see Mono#doOnSuccess(Consumer)
     * @see Template
     */
    @Nonnull
    Mono<Void> send(@Nonnull T template, @Nonnull Values context);

    /**
     * 关闭通知器,以释放相关资源
     *
     * @return 关闭结果
     */
    @Nonnull
    Mono<Void> close();

}
