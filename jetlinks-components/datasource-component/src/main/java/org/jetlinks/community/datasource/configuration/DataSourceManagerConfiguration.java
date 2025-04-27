package org.jetlinks.community.datasource.configuration;

import org.jetlinks.community.datasource.*;
import org.jetlinks.community.datasource.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DataSourceManagerConfiguration {

    @Bean
    @ConditionalOnBean(DataSourceConfigManager.class)
    public DataSourceManager dataSourceManager(DataSourceConfigManager dataSourceConfigManager,
                                               ObjectProvider<DataSourceProvider>  providers,
                                               ObjectProvider<DataSource>  dataSources){
        DefaultDataSourceManager dataSourceManager= new DefaultDataSourceManager(dataSourceConfigManager);
        providers.forEach(dataSourceManager::register);
        dataSources.forEach(dataSourceManager::register);
        return dataSourceManager;
    }

}
