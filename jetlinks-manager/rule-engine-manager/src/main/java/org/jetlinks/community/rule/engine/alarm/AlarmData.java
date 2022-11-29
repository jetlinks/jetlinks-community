package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class AlarmData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alarmConfigId;
    private String alarmName;

    private String ruleId;
    private String ruleName;

    private Map<String, Object> output;
}
