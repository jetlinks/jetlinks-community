package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SceneActions implements Serializable {

    @Schema(description = "是否并行执行动作")
    private boolean parallel;

    @Schema(description = "执行动作")
    private List<SceneAction> actions;


    //仅用于设置到reactQl sql的column中
    public List<String> createContextColumns(){
        List<String> contextTerm = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(actions)){
            for (SceneAction action : actions) {
                contextTerm.addAll(action.createContextColumns());
            }
        }
        return contextTerm;

    }

}
