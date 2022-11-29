package org.jetlinks.community.rule.engine;

import org.jetlinks.rule.engine.api.task.ExecutionContext;

import java.util.Optional;

public interface RuleEngineConstants {

    String ruleCreatorIdKey = "creatorId";

    String ruleName = "name";

    static Optional<String> getCreatorId(ExecutionContext context) {
        return context.getJob()
                      .getRuleConfiguration(ruleCreatorIdKey)
                      .map(String::valueOf);
    }

    static Optional<String> getRuleName(ExecutionContext context) {
        return context.getJob()
                      .getRuleConfiguration(ruleName)
                      .map(String::valueOf);
    }
}
