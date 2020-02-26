package org.jetlinks.community.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class RuleEngineExecuteEventInfo {

    private String event;

    private long createTime = System.currentTimeMillis();

    private String instanceId;

    private String nodeId;

    private String ruleData;
}
