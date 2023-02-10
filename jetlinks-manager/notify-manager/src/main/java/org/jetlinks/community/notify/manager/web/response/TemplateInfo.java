package org.jetlinks.community.notify.manager.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.notify.template.VariableDefinition;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateInfo {

    @Schema(description = "模版ID")
    private String id;

    @Schema(description = "模版名称")
    private String name;

    @Schema(description = "变量定义信息")
    private List<VariableDefinition> variableDefinitions;


}
