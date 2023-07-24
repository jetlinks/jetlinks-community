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
