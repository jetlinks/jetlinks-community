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
package org.jetlinks.community.rule.engine.scene.internal.triggers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.rule.engine.scene.AbstractSceneTriggerProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.sdk.server.SdkServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "rule.scene.trigger.device")
public class DeviceTriggerProvider extends AbstractSceneTriggerProvider<DeviceTrigger> {

    private static boolean sharedSupported;

    static {
        try {
            Class.forName("org.jetlinks.community.device.entity.DeviceInstanceEntity");
            sharedSupported = false;
        } catch (ClassNotFoundException e) {
            //当前不在设备模块,默认使用共享订阅
            sharedSupported = true;
        }
    }

    public static final String PROVIDER = "device";

    private final ThingsRegistry registry;

    @Getter
    @Setter
    private boolean sharedMod = sharedSupported;

    @Getter
    @Setter
    private Set<String> customHeaders = new HashSet<>();

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.scene_trigger_name_device","设备触发");
    }

    @Override
    public DeviceTrigger newConfig() {
        return new DeviceTrigger();
    }

    @Override
    public SqlRequest createSql(DeviceTrigger config, List<Term> terms, boolean hasFilter) {
        return config.createSql(terms, customHeaders, hasFilter);
    }

    @Override
    public SqlFragments createFilter(DeviceTrigger config, List<Term> terms) {
        return config.createFragments(terms);
    }

    @Override
    public Term refactorTerm(String mainTableName, Term term) {
        return DeviceTrigger.refactorTermValue(mainTableName, term);
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
