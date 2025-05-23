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
package org.jetlinks.community.gateway.supports;

import org.jetlinks.core.Wrapper;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.network.NetworkType;
import reactor.core.publisher.Mono;

/**
 * 设备网关支持提供商,用于提供对各种设备网关的支持.在启动设备网关时,会根据对应的提供商以及配置来创建设备网关.
 * 实现统一管理网关配置,动态创建设备网关.
 *
 * @author zhouhao
 * @see DeviceGateway
 * @since 1.0
 */
public interface DeviceGatewayProvider extends Wrapper {

    String CHANNEL_NETWORK = "network";

    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称
     */
    String getName();

    /**
     * @return 接入说明
     */
    default String getDescription() {
        return null;
    }

    /**
     * 接入通道,如: network,modbus
     *
     * @return 通道
     */
    default String getChannel() {
        return CHANNEL_NETWORK;
    }

    /**
     * @return 排序。从小到大排序
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * @return 传输协议
     */
    Transport getTransport();

    /**
     * 使用配置信息创建设备网关
     *
     * @param properties 配置
     * @return void
     */
    Mono<? extends DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties);

    /**
     * 重新加载网关
     *
     * @param gateway    网关
     * @param properties 配置信息
     * @return void
     */
    default Mono<? extends DeviceGateway> reloadDeviceGateway(DeviceGateway gateway, DeviceGatewayProperties properties) {
        return Mono.just(gateway);
    }
}
