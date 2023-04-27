package org.jetlinks.community.gateway;

import org.jetlinks.community.gateway.supports.*;
import org.jetlinks.community.network.channel.ChannelProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ChannelProvider.class)
public class GatewayConfiguration {


    @Bean
    @ConditionalOnBean(DeviceGatewayPropertiesManager.class)
    public DeviceGatewayManager deviceGatewayManager(ObjectProvider<ChannelProvider> channelProviders,
                                                     ObjectProvider<DeviceGatewayProvider> gatewayProviders,
                                                     DeviceGatewayPropertiesManager propertiesManager) {
        DefaultDeviceGatewayManager gatewayManager=new DefaultDeviceGatewayManager(propertiesManager);
        channelProviders.forEach(gatewayManager::addChannelProvider);
        gatewayProviders.forEach(gatewayManager::addGatewayProvider);
        gatewayProviders.forEach(DeviceGatewayProviders::register);
        return gatewayManager;
    }

    @Bean
    public ChildDeviceGatewayProvider childDeviceGatewayProvider(){
        return new ChildDeviceGatewayProvider();
    }

}
