package org.jetlinks.community.notify.dingtalk.corp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorpUser {

    @JsonProperty
    @JsonAlias("userid")
    private String id;

    @JsonProperty
    private String name;

}
