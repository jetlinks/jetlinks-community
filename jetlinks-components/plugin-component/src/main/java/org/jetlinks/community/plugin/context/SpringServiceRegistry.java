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
package org.jetlinks.community.plugin.context;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.plugin.core.ServiceRegistry;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
public class SpringServiceRegistry implements ServiceRegistry {
    private final ApplicationContext context;

    @Override
    public <T> Optional<T> getService(Class<T> aClass) {

        try {
            return Optional.of(context.getBean(aClass));
        } catch (Throwable error) {
            log.warn("get spring service [{}] error", aClass, error);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getService(Class<T> aClass, String s) {
        try {
            return Optional.of(context.getBean(s, aClass));
        } catch (Throwable error) {
            log.warn("get spring service [{}][{}] error", s, aClass, error);
            return Optional.empty();
        }
    }

    @Override
    public <T> List<T> getServices(Class<T> aClass) {
        try {
            return new ArrayList<>(context.getBeansOfType(aClass).values());
        } catch (Throwable error) {
            log.warn("get spring services  [{}] error", aClass, error);
            return Collections.emptyList();
        }
    }
}
