package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网关消息连接器,用于连接自定义到消息到网关.
 * <p>
 * 如果连接器没有任何连接订阅者,将拒绝连接{@link MessageConnection#disconnect()}.
 * <p>
 * ⚠️注意: 连接器不会保存任何连接信息
 *
 * @author zhouhao
 * @see MessageConnection
 */
public interface MessageConnector {

    /**
     * @return 连接器唯一标识
     */
    @Nonnull
    String getId();

    /**
     * @return 名称
     */
    @Nullable
    default String getName() {
        return getId();
    }

    /**
     * @return 说明
     */
    @Nullable
    default String getDescription() {
        return null;
    }

    /**
     * 订阅连接器中到网络连接,此订阅只会获取到最新的连接.
     * <p>
     * 如果订阅了多次,每个订阅都会收到每一个连接.
     * <p>
     * ⚠️: 如果发生错误不想停止订阅,请处理好{@link Flux#onErrorContinue(BiConsumer)}或者{@link Flux#onErrorResume(Function)}
     *
     * @return 网络连接流.
     * @see MessageConnection
     */
    @Nonnull
    Flux<MessageConnection> onConnection();

}
