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
