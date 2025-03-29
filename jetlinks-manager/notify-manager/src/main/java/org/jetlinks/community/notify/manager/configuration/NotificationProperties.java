package org.jetlinks.community.notify.manager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.buffer.BufferProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jetlinks.notification")
@Getter
@Setter
public class NotificationProperties {

    private BufferProperties buffer = new BufferProperties();

    public NotificationProperties(){
        buffer.setFilePath("./data/notification-buffer");
        buffer.setSize(1000);
        buffer.setTimeout(Duration.ofSeconds(1));
    }
}
