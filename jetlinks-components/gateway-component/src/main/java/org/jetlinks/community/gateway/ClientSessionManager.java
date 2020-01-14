package org.jetlinks.community.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 客户端会话管理器,用于管理客户端会话.
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface ClientSessionManager {

    /**
     * 创建一个新会话
     *
     * @param messageGatewayId 消息网关ID
     * @param connection       网关消息连接
     * @return 创建结果
     */
    Mono<ClientSession> createSession(String messageGatewayId, MessageConnection connection);

    /**
     * 获取网关内全部会话
     *
     * @param messageGatewayId 网关
     * @return 会话流
     */
    Flux<ClientSession> getSessions(String messageGatewayId);

    /**
     * 从指定的网关里获取指定的会话,如果会话不存在则返回{@link Mono#empty()}
     *
     * @param messageGatewayId 消息网关ID
     * @param sessionId        会话ID
     * @return 会话
     */
    Mono<ClientSession> getSession(String messageGatewayId, String sessionId);

    /**
     * 批量获取指定网关下的会话
     *
     * @param messageGatewayId 网关ID
     * @param sessionIdList    会话ID
     * @return 会话流
     */
    Flux<ClientSession> getSessions(String messageGatewayId, Collection<String> sessionIdList);

    /**
     * 关闭指定网关的会话
     *
     * @param messageGatewayId 消息网关ID
     * @param sessionId        会话ID
     * @return 关闭结果
     */
    Mono<Void> closeSession(String messageGatewayId, String sessionId);

}
