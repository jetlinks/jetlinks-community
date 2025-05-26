package org.jetlinks.community.device.service.tag;

import org.jetlinks.community.buffer.BufferProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "device.tag.synchronizer")
public class DeviceTagProperties extends BufferProperties {

    public DeviceTagProperties(){
        setFilePath("./data/device-tag-synchronizer");
        setSize(500);
        setParallelism(1);
        getEviction().setMaxSize(100_0000);
    }

}
