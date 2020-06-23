package org.jetlinks.community.rule.engine.model;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class Action implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 执行器
     *
     * @see RuleNodeModel#getExecutor()
     */
    private String executor;

    /**
     * 执行器配置
     *
     * @see RuleNodeModel#getConfiguration()
     */
    private Map<String, Object> configuration;
}
