package org.jetlinks.community.device.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.function.ReactorQLDeviceSelectorBuilder;
import org.jetlinks.community.device.function.RelationDeviceSelectorProvider;
import org.jetlinks.community.device.message.DeviceMessageConnector;
import org.jetlinks.community.device.message.writer.TimeSeriesMessageWriterConnector;
import org.jetlinks.community.device.service.data.*;
import org.jetlinks.community.rule.engine.executor.DeviceSelectorBuilder;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.server.MessageHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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


    @Configuration
    @ConditionalOnProperty(prefix = "jetlinks.device.storage", name = "enable-last-data-in-db", havingValue = "true")
    static class DeviceLatestDataServiceConfiguration {

        @Bean
        @ConfigurationProperties(prefix = "jetlinks.device.storage.latest.buffer")
        public BufferProperties deviceLatestDataServiceBufferProperties() {
            BufferProperties bufferProperties = new BufferProperties();
            bufferProperties.setFilePath("./data/device-latest-data-buffer");
            bufferProperties.setSize(1000);
            bufferProperties.setParallelism(1);
            bufferProperties.setTimeout(Duration.ofSeconds(1));
            return bufferProperties;
        }

        @Bean(destroyMethod = "destroy")
        public DatabaseDeviceLatestDataService deviceLatestDataService(DatabaseOperator databaseOperator) {
            return new DatabaseDeviceLatestDataService(databaseOperator,
                                                       deviceLatestDataServiceBufferProperties());
        }

    }

    @Bean
    @ConditionalOnProperty(
        prefix = "jetlinks.device.storage",
        name = "enable-last-data-in-db",
        havingValue = "false",
        matchIfMissing = true)
    @ConditionalOnMissingBean(DeviceLatestDataService.class)
    public DeviceLatestDataService deviceLatestDataService() {
        return new NonDeviceLatestDataService();
    }



}
