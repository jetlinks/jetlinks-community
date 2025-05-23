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
package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.tdengine.TDengineConfiguration;
import org.jetlinks.community.tdengine.TDengineOperations;
import org.jetlinks.community.things.data.DefaultMetricMetadataManager;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = TDengineConfiguration.class)
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
@ConditionalOnBean(TDengineOperations.class)
public class TDengineThingDataConfiguration {


    @Bean(destroyMethod = "dispose")
    public TDengineThingDataHelper tDengineThingDataHelper(TDengineOperations operations) {

        return new TDengineThingDataHelper(
            operations,
            new DefaultMetricMetadataManager()
        );
    }

    @Bean
    public TDengineColumnModeStrategy tDengineColumnModeStrategy(ThingsRegistry registry,
                                                                 TDengineThingDataHelper operations) {

        return new TDengineColumnModeStrategy(registry, operations);
    }

    @Bean
    public TDengineRowModeStrategy tDengineRowModeStrategy(ThingsRegistry registry,
                                                           TDengineThingDataHelper operations) {

        return new TDengineRowModeStrategy(registry, operations);
    }

}
