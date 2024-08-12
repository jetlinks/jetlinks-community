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
     * @see CrudCommandSupport
     */
    String networkService = "networkService";

    /**
     * 设备接入网关服务
     * @see CrudCommandSupport
     */
    String deviceGatewayService = "deviceGatewayService";


    /**
     * 采集器服务
     * @see CrudCommandSupport
     */
    String collectorService = "collectorService";

    /**
     * 规则服务
     * @see CrudCommandSupport
     */
    String ruleService = "ruleService";


    /**
     * 插件服务
     * @see CrudCommandSupport
     */
    String pluginService = "pluginService";
}
