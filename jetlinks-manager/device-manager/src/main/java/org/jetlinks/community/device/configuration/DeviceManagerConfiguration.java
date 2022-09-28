package org.jetlinks.community.device.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.function.ReactorQLDeviceSelectorBuilder;
import org.jetlinks.community.device.function.RelationDeviceSelectorProvider;
import org.jetlinks.community.device.message.DeviceMessageConnector;
import org.jetlinks.community.device.message.writer.TimeSeriesMessageWriterConnector;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.community.rule.engine.executor.DeviceSelectorBuilder;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.server.MessageHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeviceDataStorageProperties.class)
public class DeviceManagerConfiguration {


    @Bean
    public DeviceSelectorProvider relationSelectorProvider() {
        return new RelationDeviceSelectorProvider();
    }

    @Bean
    public DeviceSelectorBuilder deviceSelectorBuilder(ReactiveRepository<DeviceInstanceEntity, String> deviceRepository,
                                                       DeviceRegistry deviceRegistry) {
        return new ReactorQLDeviceSelectorBuilder(deviceRegistry, deviceRepository);
    }

    @Bean
    public DeviceMessageConnector deviceMessageConnector(EventBus eventBus,
                                                         MessageHandler messageHandler,
                                                         DeviceSessionManager sessionManager,
                                                         DeviceRegistry registry) {
        return new DeviceMessageConnector(eventBus, registry, messageHandler, sessionManager);
    }

    @Bean
    @ConditionalOnProperty(prefix = "device.message.writer.time-series", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TimeSeriesMessageWriterConnector timeSeriesMessageWriterConnector(DeviceDataService dataService) {
        return new TimeSeriesMessageWriterConnector(dataService);
    }


}
