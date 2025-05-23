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
package org.jetlinks.community.network;


/**
 * 网络服务配置,如TCP服务,MQTT服务等
 *
 * @author zhouhao
 * @see 2.0
 */
public interface ServerNetworkConfig extends NetworkConfig {


    /**
     * 提供公共访问的Host,比如 公网ip,域名,内网ip
     *
     * @return Host
     */
    String getPublicHost();

    /**
     * 提供公共访问的端口
     * 场景:经过代理后可能访问服务的端口与服务启动时绑定的端口不一致.
     *
     * @return 端口号
     */
    int getPublicPort();


    /**
     * 公网是否开启安全加密,为true时,{@link ServerNetworkConfig#getCertId()}不能为空
     *
     * @return 是否开启安全加密
     */
    boolean isPublicSecure();

    /**
     * 获取公网安全证书ID
     *
     * @return 公网证书ID
     * @see org.jetlinks.community.network.security.Certificate
     * @see org.jetlinks.community.network.security.CertificateManager
     */
    String getPublicCertId();

    /**
     * @return 公共访问地址
     */
    default String getPublicAddress() {
        return getSchema() + "://" + getPublicHost() + ":" + getPublicPort();
    }

    /**
     * 网络服务绑定到本地网卡的地址,如: 0.0.0.0
     *
     * @return 绑定网卡地址
     */
    String getHost();

    /**
     * 网络服务暴露的端口
     *
     * @return 端口号
     */
    int getPort();

    default String getLocalAddress() {
        return getSchema() + "://" + getHost() + ":" + getPort();
    }

    /**
     * 是否开启安全加密,为true时,{@link ServerNetworkConfig#getCertId()}不能为空
     *
     * @return 是否开启安全加密
     */
    boolean isSecure();

    /**
     * 获取安全证书ID
     *
     * @return 证书ID
     * @see org.jetlinks.community.network.security.Certificate
     * @see org.jetlinks.community.network.security.CertificateManager
     */
    String getCertId();

}
