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
package org.jetlinks.community.command;

/**
 * 平台内部的一些服务定义
 *
 * @author zhouhao
 * @see org.jetlinks.sdk.server.SdkServices
 * @since 2.2
 */
public interface InternalSdkServices {

    /**
     * 网络组件服务
     */
    String networkService = "networkService";

    /**
     * 设备接入网关服务
     */
    String deviceGatewayService = "deviceGatewayService";


    /**
     * 采集器服务
     */
    String collectorService = "collectorService";

    /**
     * 规则服务
     */
    String ruleService = "ruleService";


    /**
     * 插件服务
     */
    String pluginService = "pluginService";

    /**
     * 基础服务
     */
    String commonService = "commonService";
}
