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
package org.jetlinks.community.iotdb;

import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ITableSessionPool.class)
@ConditionalOnProperty(prefix = "iotdb", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(IoTDBProperties.class)
public class IoTDBConfiguration {

    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean(IoTDBOperations.class)
    public IoTDBOperations iotdbOperations(IoTDBProperties properties) {
        return new IoTDBClientOperations(properties);
    }
}
