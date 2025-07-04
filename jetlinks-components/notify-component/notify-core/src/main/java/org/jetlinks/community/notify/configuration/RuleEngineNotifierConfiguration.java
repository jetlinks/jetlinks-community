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
package org.jetlinks.community.notify.configuration;

import lombok.Generated;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.rule.NotifierTaskExecutorProvider;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass({
    TaskExecutorProvider.class
})
@Configuration
@Generated
public class RuleEngineNotifierConfiguration {

    @Bean
    @ConditionalOnBean(NotifierManager.class)
    public NotifierTaskExecutorProvider notifierTaskExecutorProvider(NotifierManager notifierManager) {
        return new NotifierTaskExecutorProvider(notifierManager);
    }
}