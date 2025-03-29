package org.jetlinks.community.device.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wangsheng
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.device.event-handler")
public class DeviceEventProperties{
    boolean offlineWhenProductDisabled;
}