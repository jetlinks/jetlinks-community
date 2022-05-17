package org.jetlinks.community.configure.device;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.guava.CaffeinatedGuava;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.jetlinks.community.micrometer.MeterRegistryManager;
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
import org.jetlinks.supports.cluster.ClusterDeviceOperationBroker;
import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
import org.jetlinks.supports.scalecube.ExtendedCluster;
import org.jetlinks.supports.server.ClusterSendToDeviceMessageHandler;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableEasyormRepository("org.jetlinks.community.configure.device.PersistentSessionEntity")
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
    public BeanPostProcessor interceptorRegister(ClusterDeviceRegistry registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DeviceMessageSenderInterceptor) {
                    registry.addInterceptor(((DeviceMessageSenderInterceptor) bean));
                }
                if (bean instanceof DeviceStateChecker) {
                    registry.addStateChecker(((DeviceStateChecker) bean));
                }
                return bean;
            }
        };
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    @ConditionalOnBean(RpcManager.class)
    public PersistenceDeviceSessionManager deviceSessionManager(RpcManager rpcManager,
                                                                ReactiveRepository<PersistentSessionEntity, String> repository) {

        return new PersistenceDeviceSessionManager(rpcManager,repository);
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
    public ClusterDeviceOperationBroker clusterDeviceOperationBroker(ExtendedCluster cluster,
                                                                     DeviceSessionManager sessionManager) {
        return new ClusterDeviceOperationBroker(cluster, sessionManager);
    }


    @Bean(initMethod = "init")
    public DeviceSessionMonitor deviceSessionMonitor(DeviceSessionManager sessionManager,
                                                     MeterRegistryManager registryManager){

        return new DeviceSessionMonitor(registryManager,sessionManager,"gateway-server-session");
    }

}
