/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.micrometer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.stream.Collectors;

@AutoConfiguration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MeterRegistryConfiguration {

    @Bean(destroyMethod = "shutdown")
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    public MeterRegistryManager meterRegistryManager(ObjectProvider<MeterRegistrySupplier> registrySuppliers,
                                                     ObjectProvider<MeterRegistryCustomizer> customizers) {
        return new MeterRegistryManager(registrySuppliers.stream().collect(Collectors.toList()),
                                        customizers.stream().collect(Collectors.toList()));
    }

}
