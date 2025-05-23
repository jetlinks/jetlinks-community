/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
