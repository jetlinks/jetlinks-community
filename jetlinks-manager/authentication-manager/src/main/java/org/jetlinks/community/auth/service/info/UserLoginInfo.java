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
package org.jetlinks.community.auth.service.info;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.logging.RequestInfo;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.core.things.Thing;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author gyl
 * @since 2.2
 */
@Getter
@Setter
public class UserLoginInfo {

    public static final PropertyConstants.Key<UserLoginInfo> CONFIG_KEY_LOGIN_TIME =
        PropertyConstants.Key.of("loginTime", null, Long.class);
    public static final PropertyConstants.Key<UserLoginInfo> CONFIG_KEY_LOGIN_IP =
        PropertyConstants.Key.of("loginIp", null, String.class);

    private static final Set<String> allConfigKey = Sets.newHashSet(
        CONFIG_KEY_LOGIN_TIME.getKey(),
        CONFIG_KEY_LOGIN_IP.getKey()
    );

    private Long loginTime;
    private String loginIp;

    public static final UserLoginInfo EMPTY = new UserLoginInfo();


    public UserLoginInfo with(RequestInfo info) {
        if (info == null) {
            return this;
        }
        this.loginIp = info.getIpAddr();//.split(",")[0];
        return this;
    }

    public Mono<Void> writeTo(Thing thing) {
        Map<String, Object> configs = FastBeanCopier.copy(this, new HashMap<>());
        return thing
            .setConfigs(configs)
            .then();
    }

    public static Mono<UserLoginInfo> readFrom(Thing thing) {
        return thing
            .getSelfConfigs(allConfigKey)
            .mapNotNull(values -> {
                if (values.isEmpty()) {
                    return null;
                }
                return FastBeanCopier.copy(values.getAllValues(), new UserLoginInfo());
            });
    }
}