package org.jetlinks.community.rule.engine.scene.internal.actions;

import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotifyActionProvider implements SceneActionProvider<NotifyAction> {
    public static final String PROVIDER = "notify";
    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public NotifyAction newConfig() {
        return new NotifyAction();
    }

    @Override
    public List<String> parseColumns(NotifyAction config) {
        return config.parseColumns();
    }

    @Override
    public Flux<Variable> createVariable(NotifyAction config) {
        return Flux.empty();
    }

    @Override
    public void applyRuleNode(NotifyAction notify, RuleNodeModel model) {
        model.setExecutor("notifier");
        Map<String, Object> config = new HashMap<>();
        config.put("notifyType", notify.getNotifyType());
        config.put("notifierId", notify.getNotifierId());
        config.put("templateId", notify.getTemplateId());
        config.put("variables", notify.getVariables());
        model.setConfiguration(config);
    }
}
