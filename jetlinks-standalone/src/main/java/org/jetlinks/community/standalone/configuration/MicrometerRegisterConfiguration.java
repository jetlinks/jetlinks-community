package org.jetlinks.community.standalone.configuration;

import io.micrometer.elastic.ElasticMeterRegistry;
import io.micrometer.elastic.ElasticNamingConvention;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Configuration
public class MicrometerRegisterConfiguration {


//    @Bean
//    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return registry -> registry.config().commonTags("server", "my服务器", "节点", "bestfeng node");
//    }

    @Bean
    MeterRegistryCustomizer<ElasticMeterRegistry> elasticMetricsNamingConvention() {
        return registry -> registry.config().namingConvention(new ElasticNamingConvention());
    }
}
