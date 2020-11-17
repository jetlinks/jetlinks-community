package org.jetlinks.community.device.configuration;

import org.jetlinks.community.device.message.DeviceMessageConnector;
import org.jetlinks.community.device.message.writer.TimeSeriesMessageWriterConnector;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceManagerConfiguration {

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
