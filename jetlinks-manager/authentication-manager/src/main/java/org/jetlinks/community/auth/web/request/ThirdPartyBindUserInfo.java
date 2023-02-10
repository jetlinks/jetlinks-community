package org.jetlinks.community.auth.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ThirdPartyBindUserInfo {
    @Schema(description = "绑定ID")
    private String id;

    @Schema(description = "平台用户ID")
    private String userId;

    @Schema(description = "第三方平台名称")
    private String providerName;

    @Schema(description = "第三方用户ID")
    private String thirdPartyUserId;

}
