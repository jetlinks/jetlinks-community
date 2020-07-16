package org.jetlinks.community.auth.service.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class SaveUserDetailRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String email;

    private String telephone;

    private String avatar;

    private String description;

}
