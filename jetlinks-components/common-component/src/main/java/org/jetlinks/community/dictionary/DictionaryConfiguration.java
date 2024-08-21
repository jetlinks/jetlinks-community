package org.jetlinks.community.dictionary;

import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.hswebframework.web.dictionary.service.DefaultDictionaryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DictionaryConfiguration {


    @Configuration
    @ConditionalOnClass(DefaultDictionaryItemService.class)
    //@ConditionalOnBean(DefaultDictionaryItemService.class)
    public static class DictionaryManagerConfiguration {


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
