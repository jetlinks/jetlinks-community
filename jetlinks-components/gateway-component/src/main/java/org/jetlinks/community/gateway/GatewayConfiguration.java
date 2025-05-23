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
