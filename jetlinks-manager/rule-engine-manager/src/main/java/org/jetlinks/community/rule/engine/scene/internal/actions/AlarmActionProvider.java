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
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.community.rule.engine.enums.AlarmMode;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
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
        return LocaleUtils
            .transform(Flux.defer(() -> Flux.fromIterable(config.createVariables())));
    }

    @Override
    public void applyRuleNode(AlarmAction config, RuleNodeModel model) {
        model.setExecutor(AlarmTaskExecutorProvider.executor);
        model.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
    }

    @Override
    public List<String> getMode() {
        return Arrays.asList(AlarmMode.trigger.name(), AlarmMode.relieve.name());
    }
}
