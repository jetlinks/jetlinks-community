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
package org.jetlinks.community.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import reactor.core.publisher.Mono;

/**
 * @author bestfeng
 */
public interface UserBindService {

    String USER_BIND_CODE_PRE = "user_bind_code_pre_";

    /**
     *  创建绑定码
     *
     * @param  userInfo 用户信息
     * @return 户绑定码
     */
    Mono<String> generateBindCode(UserInfo userInfo);

    /**
     * 根据绑定码获取用户信息
     *
     * @param bindCode 户绑定码
     */
    Mono<UserInfo> getUserInfoByCode(String bindCode);

    /**
     * 用户绑定校验，发起绑定业务用户与当前用户应一致
     */
    void checkUserBind(Authentication authentication, UserInfo userInfo);

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    class UserInfo {

        private String unionId;

        private String avatarUrl;

        private String name;

        //第三方用户Id
        private String thirdPartyUserId;

        //发起绑定业务的平台用户Id
        private String userId;
    }
}
