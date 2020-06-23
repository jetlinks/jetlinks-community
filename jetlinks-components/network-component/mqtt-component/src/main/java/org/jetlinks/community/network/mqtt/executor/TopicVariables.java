package org.jetlinks.community.network.mqtt.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.rule.engine.api.RuleDataCodec;

import java.util.List;

@Getter
@AllArgsConstructor
public class TopicVariables implements RuleDataCodec.Feature {
    List<String> variables;
}
