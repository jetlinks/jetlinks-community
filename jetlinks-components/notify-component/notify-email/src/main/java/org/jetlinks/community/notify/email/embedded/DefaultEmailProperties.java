/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.notify.email.embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
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
