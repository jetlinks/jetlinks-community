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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceDataTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.SceneAction;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.SceneUtils;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.sdk.server.SdkServices;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class DeviceDataActionProvider implements SceneActionProvider<DeviceDataActionProvider.DeviceDataAction> {
    public static final String PROVIDER = "device-data";

    private final ThingsRegistry registry;

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public DeviceDataAction newConfig() {
        return new DeviceDataAction();
    }

    @Override
    public List<String> parseColumns(DeviceDataAction config) {
        String upperKey = config.getSelector().getUpperKey();
        return upperKey == null ? Collections.emptyList() : Collections.singletonList(upperKey);
    }

    @Override
    public Flux<Variable> createVariable(DeviceDataAction config) {
        return config
            .getSelector()
            .getDeviceMetadata(registry, config.getProductId())
            .map(config::createOutputType)
            .flatMapIterable(type -> {
                List<PropertyMetadata> props = type.getProperties();
                List<Variable> variables = new ArrayList<>(props.size());
                for (PropertyMetadata prop : props) {
                    variables.add(
                        SceneAction
                            .toVariable(prop.getId(),
                                        prop.getName(),
                                        prop.getValueType(),
                                        "message.scene.action.device-data.properties",
                                        "设备[%s]信息",
                                        null)
                    );

                }
                return variables;
            });
    }

    @Override
    public void applyRuleNode(DeviceDataAction config, RuleNodeModel model) {

        model.setExecutor(DeviceDataTaskExecutorProvider.ID);
        if (DeviceSelectorProviders.isFixed(config.getSelector())) {
            SceneUtils.refactorUpperKey(config.getSelector());
            config.setSelector(FastBeanCopier.copy(config.getSelector(), new DeviceSelectorSpec()));
        } else {
            config.setSelector(
                DeviceSelectorProviders.composite(
                    //先选择产品下的设备
                    DeviceSelectorProviders.product(config.getProductId()),
                    FastBeanCopier.copy(config.getSelector(), new DeviceSelectorSpec())
                ));
        }

        model.setConfiguration(config.toMap());
    }

    @Getter
    @Setter
    public static class DeviceDataAction extends DeviceDataTaskExecutorProvider.Config {
        private String productId;
    }
}
