package org.jetlinks.community.rule.engine.scene.internal.actions;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
public class AlarmActionProvider implements SceneActionProvider<AlarmAction> {
    public static final String PROVIDER = "alarm";
    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public AlarmAction newConfig() {
        return new AlarmAction();
    }

    @Override
    public List<String> parseColumns(AlarmAction config) {
        return Collections.emptyList();
    }

    @Override
    public Flux<Variable> createVariable(AlarmAction config) {
        return Flux.fromIterable(config.createVariables());
    }

    @Override
    public void applyRuleNode(AlarmAction config, RuleNodeModel model) {
        model.setExecutor(AlarmTaskExecutorProvider.executor);
        model.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
    }
}
