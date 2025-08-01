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
package org.jetlinks.community.auth.login;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.auth.cipher.CipherProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "user.login")
public class UserLoginProperties {

    //加密相关配置
    private CipherProperties encrypt = new CipherProperties();

    //登录失败限制相关配置
    private BlockLogic block = new BlockLogic();


    @Getter
    @Setter
    public static class BlockLogic {

        //开启登录失败限制
        private boolean enabled;
        //限制作用域,默认ip+用户名
        private Scope[] scopes = Scope.values();
        //最大登录失败次数
        private int maximum = 5;

        //代理深度,默认为1,用于获取经过代理后的客户端真实IP地址
        private int proxyDepth = 1;
        //限制时间,默认10分钟
        private Duration ttl = Duration.ofMinutes(10);

        public String getRealIp(String ipAddress) {
            String[] split = ipAddress.split(",");
            if (split.length > proxyDepth) {
                return split[split.length - proxyDepth - 1].trim();
            }
            return split[split.length - 1].trim();
        }

        public boolean hasScope(Scope scope) {
            for (Scope scope1 : scopes) {
                if (scope1 == scope) {
                    return true;
                }
            }
            return false;
        }

        public enum Scope {
            ip,
            username
        }

    }
}
