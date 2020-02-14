package org.jetlinks.community.gateway.monitor;

/**
 * 设备网关监控
 */
public interface DeviceGatewayMonitor {

    /**
     * 上报总连接数
     *
     * @param total 总连接数
     */
    void totalConnection(long total);

    /**
     * 创建新连接
     */
    void connected();

    /**
     * 拒绝连接
     */
    void rejected();

    /**
     * 断开连接
     */
    void disconnected();

    /**
     * 接收消息
     */
    void receivedMessage();

    /**
     * 发送消息
     */
    void sentMessage();

}
