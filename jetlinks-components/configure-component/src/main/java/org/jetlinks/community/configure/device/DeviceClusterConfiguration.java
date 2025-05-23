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
package org.jetlinks.community.configure.device;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.guava.CaffeinatedGuava;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.device.DeviceOperationBroker;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceStateChecker;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.things.ThingRpcSupportChain;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.cluster.RpcDeviceOperationBroker;
import org.jetlinks.supports.server.ClusterSendToDeviceMessageHandler;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnBean(ProtocolSupports.class)
public class DeviceClusterConfiguration {

    @Bean
    public ClusterDeviceRegistry deviceRegistry(ProtocolSupports supports,
                                                ClusterManager manager,
                                                ConfigStorageManager storageManager,
                                                DeviceOperationBroker handler) {

        return new ClusterDeviceRegistry(supports,
                                         storageManager,
                                         manager,
                                         handler,
                                         CaffeinatedGuava.build(Caffeine.newBuilder()));
    }


    @Bean
    @ConditionalOnBean(ClusterDeviceRegistry.class)
    public SmartInitializingSingleton interceptorRegister(ApplicationContext context,
                                                          ClusterDeviceRegistry registry) {
        return ()->{

            context.getBeansOfType(DeviceMessageSenderInterceptor.class)
                   .values()
                   .forEach(registry::addInterceptor);

            context.getBeansOfType(DeviceStateChecker.class)
                   .values()
                   .forEach(registry::addStateChecker);

            context.getBeansOfType(ThingRpcSupportChain.class)
                   .values()
                   .forEach(registry::addRpcChain);

        };
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    @ConditionalOnBean(RpcManager.class)
    @ConfigurationProperties(prefix = "device.session.persistence")
    public PersistenceDeviceSessionManager deviceSessionManager(RpcManager rpcManager) {

        return new PersistenceDeviceSessionManager(rpcManager);
    }

    @ConditionalOnBean(DecodedClientMessageHandler.class)
    @Bean
    public ClusterSendToDeviceMessageHandler defaultSendToDeviceMessageHandler(DeviceSessionManager sessionManager,
                                                                               DeviceRegistry registry,
                                                                               MessageHandler messageHandler,
                                                                               DecodedClientMessageHandler clientMessageHandler) {
        return new ClusterSendToDeviceMessageHandler(sessionManager, messageHandler, registry, clientMessageHandler);
    }

    @Bean
    public RpcDeviceOperationBroker rpcDeviceOperationBroker(RpcManager rpcManager,
                                                             DeviceSessionManager sessionManager) {
        return new RpcDeviceOperationBroker(rpcManager, sessionManager);
    }


}
