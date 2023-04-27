package org.jetlinks.community.notify.wechat.corp.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import org.jetlinks.community.notify.wechat.corp.CorpTag;

import java.util.Collections;
import java.util.List;

@Setter
public class GetTagResponse extends ApiResponse {

    @JsonProperty
    @JsonAlias("taglist")
    private List<CorpTag> tagList;

    public List<CorpTag> getTagList() {
        if (tagList == null) {
            return Collections.emptyList();
        }
        return tagList;
    }
}
