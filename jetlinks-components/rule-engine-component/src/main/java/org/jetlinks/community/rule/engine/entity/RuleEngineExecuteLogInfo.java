package org.jetlinks.community.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class RuleEngineExecuteLogInfo {

    private String id;

    private String instanceId;

    private String nodeId;

    private String level;

    private String message;

    private long createTime = System.currentTimeMillis();

    private long timestamp;

    //private List<Object> args;

    private String context;
}
