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
