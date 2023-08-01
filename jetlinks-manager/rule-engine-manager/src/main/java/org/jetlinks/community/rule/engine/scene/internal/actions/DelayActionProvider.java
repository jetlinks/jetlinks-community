package org.jetlinks.community.rule.engine.scene.internal.actions;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.executor.DelayTaskExecutorProvider;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Component
public class DelayActionProvider implements SceneActionProvider<DelayAction> {

    public static final String PROVIDER = "delay";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public DelayAction newConfig() {
        return new DelayAction();
    }

    @Override
    public List<String> parseColumns(DelayAction config) {
        return Collections.emptyList();
    }

    @Override
    public Flux<Variable> createVariable(DelayAction config) {
        return Flux.empty();
    }

    @Override
    public void applyRuleNode(DelayAction delay, RuleNodeModel model) {
        DelayTaskExecutorProvider.DelayTaskExecutorConfig config = new DelayTaskExecutorProvider.DelayTaskExecutorConfig();
        config.setPauseType(DelayTaskExecutorProvider.PauseType.delay);
        config.setTimeout(delay.getTime());
        config.setTimeoutUnits(delay.getUnit().chronoUnit);
        model.setExecutor(DelayTaskExecutorProvider.EXECUTOR);
        model.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
    }
}
