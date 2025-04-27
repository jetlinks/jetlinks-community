package org.jetlinks.community.datasource.configuration;

import org.jetlinks.community.datasource.command.CommandHandlerProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;
import java.util.List;

public class DataSourceHandlerProviderRegister implements ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {

        List<CommandHandlerProvider> commandHandlerProviders = context
            .getBeanProvider(CommandHandlerProvider.class)
            .stream()
            .toList();

        commandHandlerProviders.forEach(provider -> CommandHandlerProvider.supports.register(provider.getType(), provider));
    }
}
