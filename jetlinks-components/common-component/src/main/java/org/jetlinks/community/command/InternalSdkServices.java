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
