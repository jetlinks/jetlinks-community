package org.jetlinks.community.network.http.server;

import org.jetlinks.core.message.codec.http.websocket.WebSocketSession;

import java.time.Duration;

/**
 * WebSocket客户端
 *
 * @author zhouhao
 * @since 1.0
 */
public interface WebSocketExchange extends WebSocketSession {

    /**
     * @return 客户端ID
     */
    String getId();

    /**
     * @return 连接是否正常
     */
    boolean isAlive();

    /**
     * 设置心跳超时间隔
     *
     * @param duration 间隔
     */
    void setKeepAliveTimeout(Duration duration);

    /**
     * 监听断开连接事件
     *
     * @param handler 监听器
     */
    void closeHandler(Runnable handler);

    /**
     * @return 最后一次心跳时间
     */
    long getLastKeepAliveTime();
}
