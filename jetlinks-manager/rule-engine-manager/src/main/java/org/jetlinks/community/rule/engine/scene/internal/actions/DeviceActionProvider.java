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
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.SceneActionProvider;
import org.jetlinks.community.rule.engine.scene.SceneUtils;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.sdk.server.SdkServices;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@AllArgsConstructor
public class DeviceActionProvider implements SceneActionProvider<DeviceAction> {
    public static final String PROVIDER = "device";

    private final ThingsRegistry registry;

    @Override
    public String getProvider() {
        return DeviceActionProvider.PROVIDER;
    }

    @Override
    public DeviceAction newConfig() {
        return new DeviceAction();
    }

    @Override
    public List<String> parseColumns(DeviceAction config) {

        return config.parseColumns();
    }

    @Override
    public Flux<Variable> createVariable(DeviceAction config) {
        return config
            .getDeviceMetadata(registry, config.getProductId())
            .as(LocaleUtils::transform)
            .flatMapIterable(config::createVariables);
    }

    @Override
    public void applyRuleNode(DeviceAction device, RuleNodeModel model) {
        DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig config = new DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig();

        config.setMessage(device.getMessage());

        if (DeviceSelectorProviders.isFixed(device)) {
            SceneUtils.refactorUpperKey(device);
            config.setSelectorSpec(FastBeanCopier.copy(device, new DeviceSelectorSpec()));
        } else {
            config.setSelectorSpec(
                DeviceSelectorProviders.composite(
                    //先选择产品下的设备
                    DeviceSelectorProviders.product(device.getProductId()),
                    FastBeanCopier.copy(device, new DeviceSelectorSpec())
                ));
        }

        config.setFrom("fixed");
        config.setStateOperator("direct");
        config.setProductId(device.getProductId());

        model.setExecutor(DeviceMessageSendTaskExecutorProvider.EXECUTOR);
        model.setConfiguration(config.toMap());
    }
}
