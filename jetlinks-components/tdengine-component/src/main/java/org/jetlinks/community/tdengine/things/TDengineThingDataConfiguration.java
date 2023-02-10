package org.jetlinks.community.tdengine.things;

import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.tdengine.TDengineConfiguration;
import org.jetlinks.community.tdengine.TDengineOperations;
import org.jetlinks.community.things.data.DefaultMetricMetadataManager;
import org.jetlinks.community.things.data.ThingsDataRepositoryStrategy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = TDengineConfiguration.class)
@ConditionalOnClass(ThingsDataRepositoryStrategy.class)
@ConditionalOnBean(TDengineOperations.class)
public class TDengineThingDataConfiguration {


    @Bean(destroyMethod = "dispose")
    public TDengineThingDataHelper tDengineThingDataHelper(TDengineOperations operations) {

        return new TDengineThingDataHelper(
            operations,
            new DefaultMetricMetadataManager()
        );
    }

    @Bean
    public TDengineColumnModeStrategy tDengineColumnModeStrategy(ThingsRegistry registry,
                                                                 TDengineThingDataHelper operations) {

        return new TDengineColumnModeStrategy(registry, operations);
    }

    @Bean
    public TDengineRowModeStrategy tDengineRowModeStrategy(ThingsRegistry registry,
                                                           TDengineThingDataHelper operations) {

        return new TDengineRowModeStrategy(registry, operations);
    }

}
