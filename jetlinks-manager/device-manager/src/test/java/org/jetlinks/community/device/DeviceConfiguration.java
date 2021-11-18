package org.jetlinks.community.device;


import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceConfiguration {
    @Bean
    public CompositeProtocolSupport compositeProtocolSupport(){
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.addDefaultMetadata(DefaultTransport.TCP,new JetLinksDeviceMetadata("test","test"));
        return support;
    }
}
