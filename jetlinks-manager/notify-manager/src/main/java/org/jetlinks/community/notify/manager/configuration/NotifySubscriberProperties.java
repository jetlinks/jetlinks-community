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
package org.jetlinks.community.notify.manager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangji 2023/6/15
 * @since 2.1
 */
@Getter
@Setter
@ConfigurationProperties("notify.subscriber")
public class NotifySubscriberProperties {
    private Set<String> allowAllNotifyUsers = new HashSet<>(Collections.singletonList("admin"));

    public boolean isAllowAllNotify(Authentication auth) {
        return allowAllNotifyUsers.contains(auth.getUser().getUsername());
    }
}
