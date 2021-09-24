package org.jetlinks.community.configure.r2dbc;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ConnectionPool.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({
    R2dbcPoolProperties.class,
    R2dbcProperties.class
})
@AutoConfigureBefore(R2dbcAutoConfiguration.class)
public class R2dbcPoolConfiguration {

    @Bean(destroyMethod = "dispose")
    @Primary
    ConnectionPool connectionFactory(R2dbcProperties properties,
                                     R2dbcPoolProperties poolProperties,
                                     ResourceLoader resourceLoader,
                                     ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
        ConnectionFactory connectionFactory = ConnectionFactoryBuilder
            .of(properties, () -> EmbeddedDatabaseConnection.get(resourceLoader.getClassLoader()))
            .configure((options) -> {
                for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : customizers) {
                    optionsCustomizer.customize(options);
                }
            })
            .build();
        R2dbcProperties.Pool pool = properties.getPool();

        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory);
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(pool.getMaxIdleTime()).to(builder::maxIdleTime);
        map.from(poolProperties.getMaxLifeTime()).to(builder::maxLifeTime);
        map.from(poolProperties.getMaxAcquireTime()).to(builder::maxAcquireTime);
        map.from(poolProperties.getMaxCreateConnectionTime()).to(builder::maxCreateConnectionTime);
        map.from(pool.getInitialSize()).to(builder::initialSize);
        map.from(pool.getMaxSize()).to(builder::maxSize);
        map.from(pool.getValidationQuery()).whenHasText().to(builder::validationQuery);
        map.from(poolProperties.getValidationDepth()).to(builder::validationDepth);
        map.from(poolProperties.getAcquireRetry()).to(builder::acquireRetry);

        if (StringUtils.hasText(pool.getValidationQuery())) {
            builder.validationQuery(pool.getValidationQuery());
        }

        return new ConnectionPool(builder.build());
    }

}
