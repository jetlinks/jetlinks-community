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
