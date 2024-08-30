package org.jetlinks.community.rule.engine.scene.internal.triggers;

import lombok.RequiredArgsConstructor;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.rule.engine.scene.AbstractSceneTriggerProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "rule.scene.trigger.device")
public class DeviceTriggerProvider extends AbstractSceneTriggerProvider<DeviceTrigger> {

    public static final String PROVIDER = "device";

    private final ThingsRegistry registry;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return "设备触发";
    }

    @Override
    public DeviceTrigger newConfig() {
        return new DeviceTrigger();
    }

    @Override
    public SqlRequest createSql(DeviceTrigger config, List<Term> terms, boolean hasFilter) {
        return config.createSql(terms, hasFilter);
    }

    @Override
    public SqlFragments createFilter(DeviceTrigger config, List<Term> terms) {
        return config.createFragments(terms);
    }

    @Override
    public List<Variable> createDefaultVariable(DeviceTrigger config) {
        return config.createDefaultVariable();
    }

    @Override
    public Flux<TermColumn> parseTermColumns(DeviceTrigger config) {
        return config.parseTermColumns(registry);
    }

    @Override
    public void applyRuleNode(DeviceTrigger config, RuleModel model, RuleNodeModel sceneNode) {
        config.applyModel(model, sceneNode);
    }
}
