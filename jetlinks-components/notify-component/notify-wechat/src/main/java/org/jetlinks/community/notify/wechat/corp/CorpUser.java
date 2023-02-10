package org.jetlinks.community.notify.wechat.corp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CorpUser {
    @JsonProperty
    @JsonAlias("userid")
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private List<String> department;

    @JsonProperty
    @JsonAlias("open_userid")
    private String openUserId;
}
