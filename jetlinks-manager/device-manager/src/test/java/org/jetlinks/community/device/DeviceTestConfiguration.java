package org.jetlinks.community.device;

import org.hswebframework.web.authorization.token.DefaultUserTokenManager;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.jetlinks.community.configure.cluster.ClusterConfiguration;
import org.jetlinks.community.configure.device.DeviceClusterConfiguration;
import org.jetlinks.community.elastic.search.configuration.ElasticSearchConfiguration;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.supports.device.session.LocalDeviceSessionManager;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration({
    CodecsAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    CustomCodecsAutoConfiguration.class,
    ClusterConfiguration.class,
    DeviceClusterConfiguration.class
})
public class DeviceTestConfiguration {

    @Bean
    public ProtocolSupports mockProtocolSupport(){
        return new MockProtocolSupport();
    }

    @Bean
    public UserTokenManager userTokenManager(){
        return new DefaultUserTokenManager();
    }

    @Bean
    public DeviceRegistry deviceRegistry() {

        return new InMemoryDeviceRegistry();
    }

    @Bean
    public MessageHandler messageHandler() {

        return new StandaloneDeviceMessageBroker();
    }

    @Bean
    public DeviceSessionManager deviceSessionManager() {

        return LocalDeviceSessionManager.create();
    }

    @Bean
    public ElasticSearchConfiguration searchConfiguration() {

        return new ElasticSearchConfiguration();
    }

    @Bean
    public JetLinksDeviceMetadataCodec jetLinksDeviceMetadataCodec(){
        return new JetLinksDeviceMetadataCodec();
    }

}
