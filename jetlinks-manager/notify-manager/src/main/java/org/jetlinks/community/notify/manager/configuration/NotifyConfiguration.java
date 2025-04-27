package org.jetlinks.community.notify.manager.configuration;

import org.jetlinks.community.notify.manager.service.TimeSeriesNotifyHistoryRepository;
import org.jetlinks.community.timeseries.TimeSeriesManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties({NotifySubscriberProperties.class, NotificationProperties.class})
public class NotifyConfiguration {

    @Bean
    public TimeSeriesNotifyHistoryRepository notifyHistoryRepository(TimeSeriesManager timeSeriesManager) {
        return new TimeSeriesNotifyHistoryRepository(timeSeriesManager);
    }

}
