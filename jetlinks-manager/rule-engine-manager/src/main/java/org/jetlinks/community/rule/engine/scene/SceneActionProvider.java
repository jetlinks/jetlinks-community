package org.jetlinks.community.rule.engine.scene;

import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmConstants;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;

import java.util.List;

public interface SceneActionProvider<C> {

    String getProvider();

    C newConfig();

    List<String> parseColumns(C config);

    Flux<Variable> createVariable(C config);

    void applyRuleNode(C config, RuleNodeModel model);

    default void applyFilterSpec(RuleNodeModel node, List<TermSpec> specs) {
        node.addConfiguration(
            AlarmConstants.ConfigKey.alarmFilterTermSpecs,
            SerializeUtils.convertToSafelySerializable(specs)
            );
    }
}
