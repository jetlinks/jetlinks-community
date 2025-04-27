package org.jetlinks.community.datasource.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DataSourceHandlerProviderConfiguration {

    @Bean
    public DataSourceHandlerProviderRegister dataSourceHandlerProviderRegister() {
        return new DataSourceHandlerProviderRegister();
    }

}
