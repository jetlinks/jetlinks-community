package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;
import lombok.Getter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 *
 * @author zhangji 2023/6/8
 * @since 2.1
 */
@Getter
@Generated
public class GetUserDetailResponse extends ApiResponse {

    @JsonProperty
    @JsonAlias("userid")
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String alias;

    @JsonProperty
    private List<String> department;

    @JsonProperty
    @JsonAlias("main_department")
    private String mainDepartment;

    @JsonProperty
    private Integer gender;

    @JsonProperty
    private String avatar;

    @JsonProperty
    @JsonAlias("qr_code")
    private String qrCode;

    @JsonProperty
    private String mobile;

    @JsonProperty
    private String email;

    @JsonProperty
    @JsonAlias("biz_mail")
    private String bizMail;

    @JsonProperty
    private String address;

    public String getUnionId() {
        if (StringUtils.hasText(mainDepartment)) {
            return mainDepartment;
        }
        if (CollectionUtils.isEmpty(department)) {
            return null;
        }
        return String.join(",", department);
    }

}
