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
package org.jetlinks.community.iotdb.things;

import org.jetlinks.community.iotdb.IoTDBConfiguration;
import org.jetlinks.community.iotdb.IoTDBOperations;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.jetlinks.core.things.ThingsRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = IoTDBConfiguration.class)
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
@ConditionalOnBean({IoTDBOperations.class, ThingsRegistry.class})
@ConditionalOnProperty(prefix = "iotdb.things-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IoTDBThingDataConfiguration {

    @Bean
    public IoTDBThingDataHelper ioTDBThingDataHelper(IoTDBOperations operations) {
        return new IoTDBThingDataHelper(operations);
    }

    @Bean
    public IoTDBRowModeStrategy ioTDBRowModeStrategy(ThingsRegistry registry, IoTDBThingDataHelper helper) {
        return new IoTDBRowModeStrategy(registry, helper);
    }

    @Bean
    public IoTDBColumnModeStrategy ioTDBColumnModeStrategy(ThingsRegistry registry, IoTDBThingDataHelper helper) {
        return new IoTDBColumnModeStrategy(registry, helper);
    }
}
