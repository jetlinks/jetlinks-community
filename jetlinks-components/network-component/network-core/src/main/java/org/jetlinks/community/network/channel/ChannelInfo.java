package org.jetlinks.community.network.channel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChannelInfo {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "地址信息")
    private List<Address> addresses;

    @Schema(description = "其他信息")
    private Map<String,Object> others;

}
