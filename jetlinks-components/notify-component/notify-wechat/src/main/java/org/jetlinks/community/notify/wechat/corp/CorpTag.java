package org.jetlinks.community.notify.wechat.corp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CorpTag {

    @JsonProperty
    @JsonAlias("tagid")
    @Schema(description = "ID")
    private String id;

    @JsonProperty
    @JsonAlias("tagname")
    @Schema(description = "名称")
    private String name;
}
