//package org.jetlinks.community.device;
//
//import com.github.benmanes.caffeine.cache.Caffeine;
//import com.github.benmanes.caffeine.guava.CaffeinatedGuava;
//import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
//import org.jetlinks.community.device.entity.DeviceInstanceEntity;
//import org.jetlinks.community.device.entity.DeviceProductEntity;
//import org.jetlinks.community.device.service.AutoDiscoverDeviceRegistry;
//import org.jetlinks.core.ProtocolSupports;
//import org.jetlinks.core.cluster.ClusterManager;
//import org.jetlinks.core.config.ConfigStorageManager;
//import org.jetlinks.core.device.DeviceOperationBroker;
//import org.jetlinks.supports.cluster.ClusterDeviceRegistry;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//@Configuration
//public class DeviceConfiguration {
//    @Bean
//    public ClusterDeviceRegistry clusterDeviceRegistry(ProtocolSupports supports,
//                                                       ClusterManager manager,
//                                                       ConfigStorageManager storageManager,
//                                                       DeviceOperationBroker handler) {
//
//        return new ClusterDeviceRegistry(supports,
//            storageManager,
//            manager,
//            handler,
//            CaffeinatedGuava.build(Caffeine.newBuilder()));
//    }
//
//    @Bean
//    @Primary
//    @ConditionalOnProperty(prefix = "jetlinks.device.registry", name = "auto-discover", havingValue = "enabled", matchIfMissing = true)
//    public AutoDiscoverDeviceRegistry deviceRegistry(ClusterDeviceRegistry registry,
//                                                     ReactiveRepository<DeviceInstanceEntity, String> instanceRepository,
//                                                     ReactiveRepository<DeviceProductEntity, String> productRepository) {
//        return new AutoDiscoverDeviceRegistry(registry, instanceRepository, productRepository);
//    }
//}
