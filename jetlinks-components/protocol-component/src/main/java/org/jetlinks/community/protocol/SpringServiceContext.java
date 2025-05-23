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
package org.jetlinks.community.protocol;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.spi.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class SpringServiceContext implements ServiceContext {

    private final ApplicationContext applicationContext;

    public SpringServiceContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Optional<Value> getConfig(ConfigKey<String> key) {
        return getConfig(key.getKey());
    }

    @Override
    public Optional<Value> getConfig(String key) {
        return Optional.ofNullable(applicationContext.getEnvironment()
                .getProperty(key))
                .map(Value::simple)
                ;
    }

    @Override
    public <T> Optional<T> getService(Class<T> service) {
        try {
            return Optional.of(applicationContext.getBean(service));
        } catch (Exception e) {
            log.error("load service [{}] error", service, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getService(String service) {
        try {
            return Optional.of((T)applicationContext.getBean(service));
        } catch (Exception e) {
            log.error("load service [{}] error", service, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> List<T> getServices(Class<T> service) {
        try {
            return new ArrayList<>(applicationContext.getBeansOfType(service).values());
        }catch (Exception e){
            log.error("load service [{}] error", service, e);
            return Collections.emptyList();
        }
    }

    @Override
    public <T> List<T> getServices(String service) {
        return Collections.emptyList();
    }
}
