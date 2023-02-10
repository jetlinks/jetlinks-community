package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import org.jetlinks.community.notify.wechat.corp.CorpUser;

import java.util.Collections;
import java.util.List;

@Setter
public class GetUserResponse extends ApiResponse {

    @JsonProperty
    @JsonAlias("userlist")
    private List<CorpUser> userList;

    public List<CorpUser> getUserList() {
        return userList == null ? Collections.emptyList() : userList;
    }
}
