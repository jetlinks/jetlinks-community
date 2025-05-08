package org.jetlinks.community.configuration;

import org.jetlinks.community.resource.ui.UiMenuResourceProvider;
import org.jetlinks.community.resource.ui.UiResourceProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "jetlinks.ui", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UiResourceConfiguration {


    @Bean
    public UiResourceProvider uiResourceProvider() {
        return new UiResourceProvider();
    }

    @Bean
    public UiMenuResourceProvider uiMenuResourceProvider() {
        return new UiMenuResourceProvider();
    }

}
