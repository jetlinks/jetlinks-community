package org.jetlinks.community.rule.engine.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.SimplePropertyMetadata;

/**
 *
 * @author zhangji 2025/1/22
 * @since 2.3
 */
@Getter
@Setter
public class ArrayChildTermColumnRequest {

    @Schema(description = "条件前缀")
    private String prefix;

    @Schema(description = "数组属性")
    private SimplePropertyMetadata propertyMetadata;


}
