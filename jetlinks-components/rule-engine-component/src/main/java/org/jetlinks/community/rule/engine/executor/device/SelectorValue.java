package org.jetlinks.community.rule.engine.executor.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SelectorValue {

    @Schema(description = "值")
    private Object value;

    @Schema(description = "名称")
    private String name;


}
