package org.jetlinks.community.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.core.ProtocolSupport;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Generated
public class ProtocolInfo {

    @Schema(description = "协议ID")
    private String id;

    @Schema(description = "协议名称")
    private String name;

    @Schema(description = "拓展配置信息")
    private Map<String, Object> configuration;


    @Schema(description = "说明")
    private String description;

    public static ProtocolInfo of(ProtocolSupport support) {
        return of(support.getId(), support.getName(), new HashMap<>(), support.getDescription());
    }

    public static ProtocolInfo of(ProtocolSupportEntity support) {
        return of(support.getId(), support.getName(), support.getConfiguration(), support.getDescription());
    }
}