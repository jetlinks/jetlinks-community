package org.jetlinks.community.rule.engine.scene;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class SceneData implements Serializable {

    private String id;

    private SceneRule rule;

    private Map<String,Object> output;
}
