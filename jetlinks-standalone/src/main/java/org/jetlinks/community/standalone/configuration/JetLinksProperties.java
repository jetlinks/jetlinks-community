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
package org.jetlinks.community.standalone.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Map;

@ConfigurationProperties(prefix = "jetlinks")
@Getter
@Setter
public class JetLinksProperties {

    private String serverId;

    private String clusterName ="default";

    private Map<String, Long> transportLimit;

    @PostConstruct
    @SneakyThrows
    public void init() {
        if (serverId == null) {
            serverId = InetAddress.getLocalHost().getHostName();
        }
    }
}
