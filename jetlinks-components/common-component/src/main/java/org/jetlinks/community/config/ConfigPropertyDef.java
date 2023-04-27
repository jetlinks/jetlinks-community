package org.jetlinks.community.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.core.metadata.types.StringType;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@EqualsAndHashCode(of = "key")
public class ConfigPropertyDef {

    @Schema(description = "配置key")
    private String key;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "是否只读")
    private boolean readonly;

    @Schema(description = "配置类型")
    private String type = StringType.ID;

    @Schema(description = "默认值")
    private String defaultValue;
}
