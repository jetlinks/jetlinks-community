package org.jetlinks.community.notify.email.embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
public class DefaultEmailProperties {
    private String host;

    private int port;

    private boolean ssl;

    private String username;

    private String password;

    private String sender;

    private List<ConfigProperty> properties;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfigProperty {

        private String name;

        private String value;

        private String description;
    }

    public Properties createJavaMailProperties() {

        Properties properties = new Properties();
        if (this.properties != null) {
            for (ConfigProperty property : this.properties) {
                properties.put(property.getName(), property.getValue());
            }
        }
        if(ssl){
            properties.putIfAbsent("mail.smtp.auth","true");
            properties.putIfAbsent("mail.smtp.ssl.enable","true");
        }
        return properties;
    }

}
