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
package org.jetlinks.community.dictionary;

import org.hswebframework.web.crud.events.EntityEventListenerCustomizer;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.hswebframework.web.dictionary.service.DefaultDictionaryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class DictionaryConfiguration {


    @AutoConfiguration
    @ConditionalOnClass(DefaultDictionaryItemService.class)
    //@ConditionalOnBean(DefaultDictionaryItemService.class)
    public static class DictionaryManagerConfiguration {

        @Bean
        public EntityEventListenerCustomizer dictionaryEntityEventListenerCustomizer() {
            return configure -> {
                configure.enable(DictionaryItemEntity.class);
                configure.enable(DictionaryEntity.class);
            };
        }

        @Bean
        public DictionaryEventHandler dictionaryEventHandler(DefaultDictionaryItemService service) {
            return new DictionaryEventHandler(service);
        }

        @Bean
        public DatabaseDictionaryManager defaultDictionaryManager(DefaultDictionaryItemService service) {
            DatabaseDictionaryManager dictionaryManager = new DatabaseDictionaryManager(service);
            Dictionaries.setup(dictionaryManager);
            return dictionaryManager;
        }

        @Bean
        public DictionaryColumnCustomizer dictionaryColumnCustomizer() {
            return new DictionaryColumnCustomizer();
        }


        @Bean
        @ConfigurationProperties(prefix = "jetlinks.dict")
        public DictionaryInitManager dictionaryInitManager(ObjectProvider<DictionaryInitInfo> initInfo,
                                                           DefaultDictionaryService defaultDictionaryService,
                                                           DefaultDictionaryItemService itemService) {
            return new DictionaryInitManager(initInfo, defaultDictionaryService, itemService);
        }

    }
}
